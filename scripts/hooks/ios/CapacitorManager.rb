#!/usr/bin/env ruby
#
# Copyright (c) 2022 BlackBerry Limited. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require 'xcodeproj'
require 'plist'
require 'json'
require 'fileutils'

class Capacitor

  DEFAULT_PRODUCT_ID = '$(PRODUCT_BUNDLE_IDENTIFIER)'

  LIBRARY_SEARCH_PATHS = '$(SDK_DIR)/usr/lib/swift $(TOOLCHAIN_DIR)/usr/lib/swift/$(PLATFORM_NAME) $(inherited)'
  LD_RUNPATH_SEARCH_PATHS = '/usr/lib/swift @executable_path/Frameworks'

  FRAMEWORKS = [
    'WebKit',
    'LocalAuthentication',
    'DeviceCheck',
    'CFNetwork',
    'CoreData',
    'CoreTelephony',
    'QuartzCore',
    'Security',
    'MessageUI',
    'SystemConfiguration',
    'MobileCoreServices',
    'CoreGraphics',
    'AssetsLibrary',
    'SafariServices',
    'AuthenticationServices'
  ]

  TBD_LIBS = [
    'z',
    'network'
  ]

  BUILD_CONFIGS = {
    'ENABLE_BITCODE'                        => 'NO',
    'LIBRARY_SEARCH_PATHS'                  => LIBRARY_SEARCH_PATHS,
    'ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES' => 'YES',
    'LD_RUNPATH_SEARCH_PATHS'               => LD_RUNPATH_SEARCH_PATHS,
    'VALIDATE_WORKSPACE'                    => 'YES'
  }

  CORDOVA_PLUGINS_BUILD_CONFIGS = {
    'ENABLE_BITCODE'                        => 'NO'
  }

  CORDOVA_PLUGINS_BUILD_CONFIGS_TO_BE_RESTORED = {
    'ENABLE_BITCODE'                        => 'YES'
  }

  # related to remove
  BUILD_FILES_TO_REMOVE = [
    'WebKit.framework',
    'LocalAuthentication.framework',
    'DeviceCheck.framework',
    'CFNetwork.framework',
    'CoreData.framework',
    'CoreTelephony.framework',
    'QuartzCore.framework',
    'Security.framework',
    'MessageUI.framework',
    'SystemConfiguration.framework',
    'MobileCoreServices.framework',
    'CoreGraphics.framework',
    'AssetsLibrary.framework',
    'SafariServices.framework',
    'AuthenticationServices.framework',
    'libz.tbd',
    'libnetwork.tbd'
  ]

  BUILD_CONFIGS_TO_BE_DELETED = {
    'LIBRARY_SEARCH_PATHS' => LIBRARY_SEARCH_PATHS
  }

  BUILD_CONFIGS_TO_BE_RESTORED = {
    'ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES' => 'NO',
    'LD_RUNPATH_SEARCH_PATHS'               => LD_RUNPATH_SEARCH_PATHS,
    'VALIDATE_WORKSPACE'                    => 'NO'
  }

  def initialize options
   @root = options[:path]
   @plist_config = generate_plist_config options[:bundle_id]
   @product_name = "App"
   @xcodeproj_path = "#{@root}/App/App.xcodeproj"
   @pods_xcodeproj_path = "#{@root}/App/Pods/Pods.xcodeproj"
   @plist_path = "#{@root}/App/App/Info.plist"
   @options = options
  end

  public

  def install
   @xcodeproj = Xcodeproj::Project.open @xcodeproj_path
   @pods_xcodeproj = Xcodeproj::Project.open @pods_xcodeproj_path
   set_bundle_id @options[:bundle_id] # set bundle id with every $ ionic cap sync

   @plist = Plist.parse_xml @plist_path
   @native_target = get_native_target

   patch_plist
   add_bbd_stuff
  end

  def uninstall
   @xcodeproj = Xcodeproj::Project.open @xcodeproj_path
   @pods_xcodeproj = Xcodeproj::Project.open @pods_xcodeproj_path
   @plist = Plist.parse_xml @plist_path
   @native_target = get_native_target

   restore_plist
   restore_xcodeproj

   @xcodeproj.save
  end

  private

  def set_bundle_id bundle_id
    config = { "PRODUCT_BUNDLE_IDENTIFIER" => bundle_id }
    @native_target = get_native_target
    @native_target.build_configurations.each do |configuration|
      configuration.build_settings.merge! config
    end

    @xcodeproj.save
  end

  def patch_plist
    @plist.merge! @plist_config

    File.open(@plist_path, 'w') do |file|
      file.write @plist.to_plist
    end
  end

  def add_bbd_stuff
   @native_target.add_system_frameworks FRAMEWORKS
   @native_target.add_system_libraries_tbd TBD_LIBS

   @native_target.build_configurations.each do |configuration|
      configuration.build_settings.merge! BUILD_CONFIGS
   end

   @xcodeproj.save

   # setting 'Enable Bitcode' to 'NO' for Pods
   set_build_configuration CORDOVA_PLUGINS_BUILD_CONFIGS

   @pods_xcodeproj.save
  end

  def set_build_configuration configs
    @pods_xcodeproj.targets.select do |target|
       target.build_configurations.each do |configuration|
          configuration.build_settings.merge! configs
       end
    end
  end

  def get_native_target
    target = @xcodeproj.targets.select do |target|
      target.name == @product_name
    end.first
    if target.nil? then
      target = @xcodeproj.targets.select.first
      @product_name = target.name
    end

    target
  end

  def generate_plist_config bundle_id
    config = {
      "GDApplicationID"          => bundle_id,
      "GDApplicationVersion"     => '1.0.0.0',
      "NSFaceIDUsageDescription" => 'Enable authentication without a password.',
      "NSCameraUsageDescription" => 'Allow camera usage to scan a QR code',
      "GDFetchResources"         => 'YES',
      "CFBundleURLTypes"         => [{
        "CFBundleURLName"        => bundle_id,
        "CFBundleURLSchemes"     => [
          "#{bundle_id}.sc2.1.0.0.0",
          "#{bundle_id}.sc2",
          "#{bundle_id}.sc3.1.0.0.0",
          "#{bundle_id}.sc3",
          'com.good.gd.discovery'
        ]
      }],
      "BlackBerryDynamics"       => {
        "CheckEventReceiver"     => false
      }
     }
  end

  def restore_plist
   @plist.delete_if {|key, value| @plist_config.has_key?(key)}

   File.open(@plist_path, 'w') do |file|
     file.write @plist.to_plist
   end
  end

  def restore_xcodeproj
    # remove frameworks
    frameworks = @xcodeproj.groups.select do |group|
      group.name == "Frameworks"
    end.first
    frameworks.clear

    @native_target.frameworks_build_phase.files.objects.each do |bf|
      if BUILD_FILES_TO_REMOVE.include? bf.display_name
        @native_target.frameworks_build_phase.remove_build_file(bf)
      end
    end

    # remove some build configuration
    @native_target.build_configurations.each do |configuration|
      configuration.build_settings.delete_if {|key, value| BUILD_CONFIGS_TO_BE_DELETED.has_key?(key)}
    end

    # restore some build configuration to default value
    @native_target.build_configurations.each do |configuration|
      configuration.build_settings.merge! BUILD_CONFIGS_TO_BE_RESTORED
    end

    @xcodeproj.save

    # restore Pods -> setting 'Enable Bitcode' to 'YES' for Pods
    set_build_configuration CORDOVA_PLUGINS_BUILD_CONFIGS_TO_BE_RESTORED

    @pods_xcodeproj.save
  end
end
