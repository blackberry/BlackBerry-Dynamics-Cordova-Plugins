#!/usr/bin/env node

/**
 * Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function(argv) {
   const path = require('path');
   const fse = require('fs-extra');
   const bbdBasePath = path.resolve('node_modules', 'capacitor-plugin-bbd-base');
   const { dynamicsPodSpec } = require(path.join(bbdBasePath, 'package.json'));
   const pattern = /pod 'BlackBerryDynamics', (:podspec|:path) => '(.+)'/;

   const parse = (arg, options = { value: null }) => {
      const argument = {
         index: null,
         value: null
      };

      argument.index = argv.indexOf(arg);
      if (argument.index  > -1) {
         argument.value = options.value ? options.value : argv[argument.index + 1];
      }

      return argument.value;
   };

   const copyPodspecFile = (_path) => {
      const specPath = path.join(_path, 'BlackBerryDynamics.podspec');
      if (fse.existsSync(specPath))
         return;
      const pathToInnerSpec = path.join(bbdBasePath, 'scripts', 'hooks', 'ios', 'BlackBerryDynamics.podspec');
      fse.copyFileSync(pathToInnerSpec, specPath);
   }

   const pod = {
      path: parse('--path'),
      url: parse('--url'),
      default: parse('--default', { value: true })
   }

   let spec;
   if (pod.default) {
      spec = `pod 'BlackBerryDynamics', :podspec => '${dynamicsPodSpec}'`;
   }
   if (pod.path) {
      copyPodspecFile(pod.path);
      pod.path = path.join(pod.path, 'BlackBerryDynamics.podspec');

      spec = `pod 'BlackBerryDynamics', :path => '${pod.path}'`;
   }
   if (pod.url) {
      spec = `pod 'BlackBerryDynamics', :podspec => '${pod.url}'`;
   }

   const rootPodFilePath = path.resolve('ios', 'App', 'Podfile');
   let rootPodFileContext = fse.readFileSync(rootPodFilePath, { encoding: 'utf-8' });
   rootPodFileContext = rootPodFileContext.replace(pattern, spec);

   fse.writeFileSync(rootPodFilePath, rootPodFileContext, { encoding: 'utf-8' });

   console.log('\x1b[32m%s\x1b[0m', 'BlackBerryDynamics podspec in Podfile was successfully updated.');
})(process.argv);
