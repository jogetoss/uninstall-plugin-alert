# Description

Uninstall Plugin Alert warns admins before they uninstall a plugin that is still being used by published apps, so apps don't break by accident. Works with Joget DX 9.1 or later.

## Features
* Warns you when you uninstall a plugin that published apps are using.
* Lets you check which apps use a plugin at any time, without uninstalling it.
* Links each app in the list straight to its builder page so you can review it first.
* Only available to admin users.

## How to Use
1. Install the plugin. No configuration is needed.
2. To uninstall safely: go to **Settings > Manage Plugins**, select the plugin(s) and click **Uninstall Plugin**. If any published apps use them, a confirmation dialog lists those apps. Click an app name to open it in the builder, then choose to continue or cancel.
3. To just check usage: in **Manage Plugins**, click a plugin to open its details, then click a plugin in the list to see which published apps use it.

## How It Works
The plugin looks at the plugins included in each published app and matches them against the plugin you selected. The match is done by plugin name, so a different version of the same plugin is still counted as in use. A plugin included in an app is listed even if nothing in that app actually uses it.

# Getting Help

JogetOSS is a community-led team for open source software related to the [Joget](https://www.joget.org) no-code/low-code application platform.
Projects under JogetOSS are community-driven and community-supported.
To obtain support, ask questions, get answers and help others, please participate in the [Community Q&A](https://answers.joget.org/).

# Contributing

This project welcomes contributions and suggestions, please open an issue or create a pull request.

Please note that all interactions fall under our [Code of Conduct](https://github.com/jogetoss/repo-template/blob/main/CODE_OF_CONDUCT.md).

# Licensing

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

NOTE: This software may depend on other packages that may be licensed under different open source licenses.
