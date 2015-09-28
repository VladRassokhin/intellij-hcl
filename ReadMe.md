# IntelliJ-HCL plugin

Provides [HCL language](https://github.com/hashicorp/hcl) and [Terraform](https://terraform.io) configuration files (`.tf`) support for [IntelliJ Platform](http://www.jetbrains.org/pages/viewpage.action?pageId=983889) based IDEs

Also HCL format is used for [Nomad](https://www.nomadproject.io/)(`.nomad` files) and [Otto](https://www.ottoproject.io/)(`Appfile` files) tools.

[Plugin page](https://plugins.jetbrains.com/plugin/7808) in [IntelliJ platform plugin repository](https://plugins.jetbrains.com).

### Features:
##### For both .hcl and .tf file formats:
* Syntax highlighting
* Structure outline in 'Structure' tool window
* Code formatter, so reformat code action available
* Code folding
* Comment/Uncomment action

#### Terraform configs (.tf) files
* Interpolations syntax highlighting
* (WIP) Properties validation (according to required properties for resource/provider, type checking)

#### Terraform configs Interpolation Language
* Syntax highlighting



### Planned features:
#### Terraform configs (.tf) files
* Go to definition from resource to provider
* Find usages for resources, providers, variables

#### Terraform configs Interpolation Language
* [Predefined methods](https://www.terraform.io/docs/configuration/interpolation.html) autocompletion
* Go to declaration on resources, providers, properties, etc.