# IntelliJ-HCL plugin

Provides [HCL language](https://github.com/hashicorp/hcl) and [Terraform](https://terraform.io) configuration files (`.tf`) support for [IntelliJ Platform](https://www.jetbrains.org/pages/viewpage.action?pageId=983889)-based IDEs

The HCL format is used for [Nomad](https://www.nomadproject.io/)(`.nomad` files).

[Plugin page](https://plugins.jetbrains.com/plugin/7808) in [IntelliJ platform plugin repository](https://plugins.jetbrains.com).

## DEPRECATION NOTICE
This repository is no longer maintained. 
This project contains the original source code of the intellij-hcl plugin. The code in this repository is kept static and can be used for historical reference only: no pull requests will be accepted.

TL/DR. Starting from plugin version 0.7.0, the sources belong to JetBrains and the plugin is closed-source. You can download the new versions of the **free** plugin from JetBrains from the [JetBrains plugin repository](https://plugins.jetbrains.com/plugin/7808).

### Note from Plugin Author

I'd been developing this plugin for four years in my free time before last year, when it became obvious that due to lack of free time I could not keep up with the newest Terraform features.

Meanwhile, IaaC and especially Terraform is becoming more and more popular, so I decided to transfer the plugin ownership to JetBrains. I believe that JetBrains has the resources to improve the plugin over time and provide efficient user support.

I'd like to thank all users, contributors, people who spotted and reported bugs, and everyone who promoted the plugin all those years.

As part of the transition, the source code was moved to the main IntelliJ repository which is private. The JetBrains plugin is  **[free](https://plugins.jetbrains.com/plugin/7808)**. The main plugin features will be compatible with JetBrains' free IDEs (\[IntelliJ IDEA, PyCharm\]x\[Community, Education\] Editions) as well as Google's Android Studio. Note that the main features include but are not limited to the features below.
 


### Features:
##### For both .hcl and .tf file formats:
* Syntax highlighting
* Structure outline in the 'Structure' tool window
* Code formatter with the 'Reformat code' action available
* Code folding
* Comment/Uncomment action

#### Terraform configs (.tf) files
* Interpolations syntax highlighting
* (WIP) Properties validation (according to the properties required for resource/provider, type checking)
* (WIP) Go to definition from resource to provider

#### Terraform configs Interpolation Language
* Syntax highlighting
* Autocompletion for [predefined methods](https://www.terraform.io/docs/configuration/interpolation.html) 
* (WIP) Go to declaration on resources, providers, properties, etc.


### Planned features:
#### Terraform configs (.tf) files
* Find usages for resources, providers, variables

#### Terraform configs Interpolation Language


### Terraform External Metadata

Starting from version 0.6.14 it's possible to use external source of Terraform model.
Previously plugins updates were necessary once something was updated in Terraform itself or providers.

Plugin reads metadata from specially-formatted json files located at (in order):
 * `TERRAFORM_GLOBAL_DIR/schemas` (intended for schemas for your custom providers/provisioners) and 
 * `TERRAFORM_GLOBAL_DIR/metadata-repo/terraform/model` (semi-automatically updated schemas) and
 * Plugin itself

Here `TERRAFORM_GLOBAL_DIR` stands for `$HOME/.terraform.d` on Linux/macOS and `%APPDATA%/terraform.d` on Windows.

:information_source: Recommended approach is to clone [special repo](https://github.com/VladRassokhin/terraform-metadata) as `TERRAFORM_GLOBAL_DIR/metadata-repo` 
and later update it from time to time.

:warning: As of plugin version 0.6.14 IntelliJ restart is required once metadata is updated on disk

Linux/macOS user may use commands like:
```bash
# To initial clone
mkdir -p "$HOME/.terraform.d/"
git clone https://github.com/VladRassokhin/terraform-metadata "$HOME/.terraform.d/metadata-repo"

# To update metadata
git -C "$HOME/.terraform.d/metadata-repo" pull
# Don't forget to restart IntelliJ after that
```
