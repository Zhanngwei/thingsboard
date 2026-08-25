# Kafka EFAK Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely correct the Kafka advertised listener, preserve all existing broker data, and deploy EFAK 3.0.1 on port 8048.

**Architecture:** Extend the existing `/opt/austin/kafka/docker-compose.yml` project. Migrate Kafka from its anonymous `/kafka` volume to an explicit `kafka_data` volume with a stable log directory, then add EFAK to the same Compose network and persist its SQLite database on the host.

**Tech Stack:** Docker Engine 20.10.17, Docker Compose 1.24.1, Kafka 2.8.1, ZooKeeper, EFAK 3.0.1, PowerShell/OpenSSH.

## Global Constraints

- Target host is `192.168.61.127`; SSH user is `itcast`.
- Keep Kafka Manager running as a fallback.
- Preserve the existing topics `__consumer_offsets`, `austinBusiness`, `austinRecall`, and `austinTraceLog`.
- EFAK image must be pinned to `nickzurich/efak:3.0.1`.
- EFAK must listen on host port `8048` and use `zookeeper:2181/kafka`.
- Kafka must advertise `PLAINTEXT://192.168.61.127:9092`.
- Kafka must publish JMX on `192.168.61.127:9999` so EFAK can probe the broker.
- Do not commit repository files unless explicitly requested.

---

### Task 1: Prepare Images and Backups

**Files:**
- Read: `/opt/austin/kafka/docker-compose.yml`
- Create: `/opt/austin/kafka/docker-compose.yml.bak-20260723`
- Create: Docker volume `kafka_data`

**Interfaces:**
- Consumes: existing Compose project `kafka` and current anonymous Kafka volume.
- Produces: pulled EFAK image, Compose backup, and empty named volume ready for migration.

- [ ] **Step 1: Reconfirm pre-change state**

Run remotely:

```bash
cd /opt/austin/kafka
sudo docker-compose ps
sudo docker inspect --format '{{range .Mounts}}{{if eq .Destination "/kafka"}}{{.Name}}{{end}}{{end}}' kafka
sudo docker exec kafka sh -lc "/opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper:2181/kafka --list"
```

Expected: Kafka, ZooKeeper, and Kafka Manager are running; an anonymous volume name is printed; the four existing topics are listed.

- [ ] **Step 2: Pull EFAK before downtime**

Run remotely:

```bash
sudo docker pull nickzurich/efak:3.0.1
```

Expected: image pull exits with status 0 and reports tag `3.0.1`.

- [ ] **Step 3: Back up Compose configuration**

Run remotely:

```bash
sudo cp -a /opt/austin/kafka/docker-compose.yml /opt/austin/kafka/docker-compose.yml.bak-20260723
sudo docker volume create kafka_data
sudo mkdir -p /opt/austin/kafka/efak/db
```

Expected: backup file, named volume, and EFAK database directory exist.

### Task 2: Migrate Kafka Data Safely

**Files:**
- Read: Docker anonymous volume mounted at `/kafka`
- Create: Docker volume `kafka_data`
- Create: `kafka_data:/kafka/kafka-logs`

**Interfaces:**
- Consumes: anonymous volume name captured in Task 1.
- Produces: a complete stopped-state copy in `kafka_data` with stable directory `/kafka/kafka-logs`.

- [ ] **Step 1: Stop only Kafka**

Run remotely:

```bash
cd /opt/austin/kafka
sudo docker-compose stop kafka
```

Expected: Kafka stops while ZooKeeper and Kafka Manager remain untouched.

- [ ] **Step 2: Copy and normalize broker data**

Run remotely, replacing `$OLD_VOLUME` with the value captured in Task 1:

```bash
sudo docker run --rm --entrypoint /bin/sh \
  -v "$OLD_VOLUME:/from:ro" \
  -v kafka_data:/to \
  wurstmeister/kafka \
  -c 'set -e; cp -a /from/. /to/; old=$(find /to -maxdepth 1 -type d -name "kafka-logs-*" | head -1); test -n "$old"; mv "$old" /to/kafka-logs'
```

Expected: command exits 0 and `/to/kafka-logs/meta.properties` exists in the named volume.

- [ ] **Step 3: Compare migration contents**

Run remotely:

```bash
sudo docker run --rm --entrypoint /bin/sh -v "$OLD_VOLUME:/from:ro" -v kafka_data:/to wurstmeister/kafka \
  -c 'test -f /to/kafka-logs/meta.properties; echo source_files=$(find /from -type f | wc -l); echo target_files=$(find /to -type f | wc -l)'
```

Expected: `meta.properties` exists and target file count is not lower than source file count.

### Task 3: Update Compose Configuration

**Files:**
- Modify: `/opt/austin/kafka/docker-compose.yml`

**Interfaces:**
- Consumes: `kafka_data`, host directory `/opt/austin/kafka/efak/db`, existing services.
- Produces: corrected Kafka configuration and a new `efak` service.

- [ ] **Step 1: Change Kafka environment and volume**

Apply these exact Kafka service changes:

```yaml
    volumes:
      - "/etc/localtime:/etc/localtime"
      - "kafka_data:/kafka"
    environment:
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://192.168.61.127:9092
      KAFKA_LOG_DIRS: /kafka/kafka-logs
      JMX_PORT: 9999
      KAFKA_JMX_OPTS: >-
        -Dcom.sun.management.jmxremote
        -Dcom.sun.management.jmxremote.authenticate=false
        -Dcom.sun.management.jmxremote.ssl=false
        -Djava.rmi.server.hostname=192.168.61.127
        -Dcom.sun.management.jmxremote.rmi.port=9999
```

Add the JMX host mapping under Kafka ports:

```yaml
    ports:
      - "9092:9092"
      - "9999:9999"
```

- [ ] **Step 2: Add EFAK service**

Add this service after `kafka-manager`:

```yaml
  efak:
    image: nickzurich/efak:3.0.1
    container_name: kafka-efak
    environment:
      EFAK_CLUSTER_ZK_LIST: zookeeper:2181/kafka
      EFAK_CLUSTER_KAFKA_EAGLE_BROKER_SIZE: 1
      EFAK_CLUSTER_KAFKA_EAGLE_OFFSET_STORAGE: kafka
      EFAK_METRICS_CHARTS: "false"
      EFAK_DB_URL: jdbc:sqlite:/hadoop/efak/db/ke.db
    volumes:
      - "./efak/db:/hadoop/efak/db"
      - "/etc/localtime:/etc/localtime:ro"
    ports:
      - "8048:8048"
    depends_on:
      - kafka
    restart: always
```

- [ ] **Step 3: Declare the external Kafka volume**

Add at the top level:

```yaml
volumes:
  kafka_data:
    external: true
```

- [ ] **Step 4: Validate Compose syntax before installation**

Run remotely against the candidate file:

```bash
cd /opt/austin/kafka
docker-compose -f /tmp/docker-compose.yml.codex config >/tmp/docker-compose.configured.yml
```

Expected: exit status 0 and rendered services include `zookepper`, `kafka`, `kafka-manager`, and `efak`.

### Task 4: Restart Kafka and Start EFAK

**Files:**
- Install: `/opt/austin/kafka/docker-compose.yml`

**Interfaces:**
- Consumes: validated Compose file and migrated `kafka_data` volume.
- Produces: corrected Kafka broker and running `kafka-efak` container.

- [ ] **Step 1: Install configuration and recreate Kafka**

Run remotely:

```bash
sudo install -m 0644 /tmp/docker-compose.yml.codex /opt/austin/kafka/docker-compose.yml
cd /opt/austin/kafka
sudo docker-compose up -d --force-recreate --no-deps kafka
```

Expected: Kafka container is recreated with `kafka_data:/kafka` and reaches `running`.

- [ ] **Step 2: Wait for Kafka metadata access**

Run remotely until successful or 60 seconds elapse:

```bash
sudo docker exec kafka sh -lc "/opt/kafka/bin/kafka-topics.sh --bootstrap-server 192.168.61.127:9092 --list"
```

Expected: the existing topics are listed, proving both advertised address and migrated data work.

- [ ] **Step 3: Start EFAK**

Run remotely:

```bash
cd /opt/austin/kafka
sudo docker-compose up -d --no-deps efak
```

Expected: container `kafka-efak` reaches `running` and binds `0.0.0.0:8048`.

### Task 5: Verify End-to-End Behavior

**Files:**
- Read: Kafka and EFAK runtime state and logs

**Interfaces:**
- Consumes: running Kafka and EFAK containers.
- Produces: fresh evidence that the deployment meets every acceptance criterion.

- [ ] **Step 1: Verify containers and mounts**

Run remotely:

```bash
sudo docker ps --filter name=kafka --filter name=kafka-efak --format '{{.Names}}|{{.Status}}|{{.Ports}}'
sudo docker inspect --format '{{range .Mounts}}{{.Name}}|{{.Source}}|{{.Destination}}{{println}}{{end}}' kafka kafka-efak
```

Expected: Kafka and EFAK remain running; Kafka uses `kafka_data`; EFAK uses the host SQLite directory.

- [ ] **Step 2: Verify HTTP internally and externally**

Run remotely and locally:

```bash
curl -fsS -o /dev/null -w 'HTTP %{http_code}\n' http://127.0.0.1:8048/
```

```powershell
(Invoke-WebRequest -UseBasicParsing -Uri 'http://192.168.61.127:8048/' -TimeoutSec 8).StatusCode
```

Expected: both checks report HTTP 200.

- [ ] **Step 3: Verify EFAK startup and cluster discovery**

Run remotely:

```bash
sudo docker logs --tail 150 kafka-efak
```

Expected: Web server starts on `8048`, ZooKeeper connection uses `zookeeper:2181/kafka`, and no fatal startup exception appears.

The Kafka Broker znode must report `"jmx_port":9999`, and EFAK logs must not contain `port out of range:-1` after the final restart.

- [ ] **Step 4: Verify existing topics remain available**

Run remotely:

```bash
sudo docker exec kafka sh -lc "/opt/kafka/bin/kafka-topics.sh --bootstrap-server 192.168.61.127:9092 --list"
```

Expected: `__consumer_offsets`, `austinBusiness`, `austinRecall`, and `austinTraceLog` are present.

## Rollback

If Kafka fails after the change, keep the migrated `kafka_data` mount and stable log directory, revert only the advertised listener, and remove EFAK:

```bash
cd /opt/austin/kafka
sudo sed -i 's#PLAINTEXT://192.168.61.127:9092#PLAINTEXT://192.168.61.128:9092#' docker-compose.yml
sudo docker-compose up -d --force-recreate --no-deps kafka
sudo docker-compose rm -sf efak 2>/dev/null || true
```
