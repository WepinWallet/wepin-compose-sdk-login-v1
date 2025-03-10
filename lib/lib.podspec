Pod::Spec.new do |spec|
    spec.name                     = 'lib'
    spec.version                  = '1.0.0'
    spec.homepage                 = 'https://github.com/WepinWallet/wepin-compose-sdk-login-v1'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Wepin Login Library'
    spec.vendored_frameworks      = 'build/cocoapods/framework/lib.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '13.0'
    spec.dependency 'AppAuth', '~> 1.7.5'
    spec.dependency 'JFBCrypt', '~> 0.1'
    spec.dependency 'secp256k1', '~> 0.1.0'
                
    if !Dir.exist?('build/cocoapods/framework/lib.framework') || Dir.empty?('build/cocoapods/framework/lib.framework')
        raise "

        Kotlin framework 'lib' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :lib:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
                
    spec.script_phases = [
        {
            :name => 'Build lib',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
    spec.resources = ['build/compose/cocoapods/compose-resources']
    spec.pod_target_xcconfig = {
            'KOTLIN_PROJECT_PATH' => ':lib',
            'PRODUCT_MODULE_NAME' => 'lib',
            'FRAMEWORK_SEARCH_PATHS' => '$(inherited) ${PODS_ROOT}/AppAuth'
        }
end