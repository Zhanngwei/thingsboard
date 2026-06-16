/*
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');

let _projectRoot = null;


(async() => {
    await moveIfExists([
        'thingsboard-js-executor-linux',
        'thingsboard-js-executor'
    ], path.join(targetPackageDir('linux'), 'bin', 'tb-js-executor'));
    await moveIfExists([
        'thingsboard-js-executor-win.exe',
        'thingsboard-js-executor.exe'
    ], path.join(targetPackageDir('windows'), 'bin', 'tb-js-executor.exe'));
})();


function projectRoot() {
    if (!_projectRoot) {
        _projectRoot = __dirname;
    }
    return _projectRoot;
}

function targetPackageDir(platform) {
    return path.join(projectRoot(), 'target', 'package', platform);
}

async function moveIfExists(fileNames, destination) {
    for (const fileName of fileNames) {
        const source = path.join(projectRoot(), 'target', fileName);
        if (fs.existsSync(source)) {
            await fse.move(source, destination, {overwrite: true});
            return;
        }
    }
}
