## Full release change-notes history

### For newest releases see `<change-notes>` in [plugin.xml](https://github.com/VladRassokhin/intellij-hcl/blob/master/res/META-INF/plugin.xml)

#### 0.5.12.2

*   Updated terraform model data to match Terraform v0.8.6  
*   Minor internal refactoring, plugin size reduced a bit  

#### 0.5.12.1

*   Support 'terraform' blocks in .tf files (#68)  
*   Updated terraform model data to match Terraform v0.8.4  

#### 0.5.12

*   Allow 'self' references in interpolation in resource connection blocks (#57)  
*   Support comparison and boolean operations in interpolation (#59)  
*   Report incorrect argument types for comparison, ternary and boolean operations in interpolations (simple cases only)  
*   Properly insert closing curve brace once interpolation start typed (#63)  
*   Fixed 'module' keyword highlighting and completion in interpolations (#62)  
*   Completion for module output in interpolations, module inputs no longer advised in interpolations completion (#52)  
*   Added completion for module parameters as well as 'missing parameter' inspection (#65, #66)  
*   Updated terraform model data to match Terraform v0.8.1  

#### 0.5.11.1

*   Fixed IAE (#55)  
*   Fixed incorrect backslash escape handling (#56)  
*   Removed Otto support since that tool was decommissioned  

#### 0.5.11

*   Support Terraform 0.8.0  
*   'Missing required property' inspection now ignores required properties with 'default' value. (#53)  
*   Multiline string literals supported only if there's interpolation inside.  
*   Backslash escaping changed in interpolations to match Terraform 0.8 style. [Details in HCL repo](https://github.com/hashicorp/hcl/pull/150).  
*   Do not highlight interpolation elements as unresolved reference for some holders, e.g. `VAR` in `data.consul.NAME.var.VAR` (#51)  
*   Updated terraform model data to match Terraform v0.8.0-rc1  

#### 0.5.10

*   'Copy Reference' action would copy FQN of resource, provider, etc. (#48)  
*   Updated terraform model data to match Terraform 0.7.5 (#49)  

#### 0.5.9

*   Updated terraform model data to match Terraform 0.7.4  

#### 0.5.8

*   Add references and completion variants for 'depends_on' in resources and data sources (#43)  
*   Updated terraform model data to match Terraform 0.7.2  

#### 0.5.7

*   Fixed incorrect inspection in .tfvars files for numeric values (#41)  
*   Fixed double quote inserter in Terraform files, now closing double quote would be added automatically if needed  
*   Improved missing properties generation in 'Missing required property' inspection based on expected property type  
*   Improved property name completion elements presentation  
*   Variable type could be autocompleted from three variants: string, list or map  
*   Variable type inspection (for 'type' and 'default' properties)  
*   Report duplicated providers with same name/alias as error  
*   Report usage of interpolations in variables, 'depends_on' and module source as error  

#### 0.5.6

*   Fixed incorrect inspection in .tfvars files (#39)  
*   Support references to elements of 'map' variables in .tfvars files (#41)  
*   Code completion in .tfvars files (#41)  
*   Support multiline interpolations in heredocs (#40)  

#### 0.5.5

*   Support Terraform 0.7.0 (issues: #31, #33, #36, #37)  
*   Updated terraform model data to match Terraform 0.7.0  

#### 0.5.4

*   Fixed error on rename refactoring (issue #29)  
*   Fixed parsing heredocs with indented end token (issue #30)  
*   Fixed interpolation escape (`$$`) (issue #34)  
*   Updated terraform model data to match Terraform 0.6.16  

#### 0.5.3

*   Updated terraform model data to match Terraform 0.6.15 (issue #28)  

#### 0.5.2

*   Updated terraform model data to match Terraform 0.6.14  
*   Support indented heredocs (issue #27)  
*   Support '.tfvars' files (issue #24)  
*   Fixed exception during formatting a .tf file with invalid syntax (issue #26)  

#### 0.5.1

*   Updated terraform model data to match Terraform 0.6.12  
*   Fix references to resource 'id' property reported as unresolved (issue #21)  
*   Fixes around unary operators in HIL  
*   Support indexing into variables in HIL ('${a[2]}') (issue #19)  
*   Various fixes  

#### 0.5.0

*   Customize highlighting text attributes (colour, etc)  
*   Improved Find Usages, Go to definition, Rename refactoring support.  
*   Support custom language injections in heredoc (issue #17)  
*   Added inspections in Terraform interpolations: incorrect use of 'self', 'unknown resource type', **'unresolved reference'**  
*   Added actions into 'Generate ...' popup. Use with `alt+insert`(`ctrl+n` on OSX)  

#### 0.4.4

*   Added 'deprecated parameter or block type' inspection.  
*   Updated terraform model data to match Terraform 0.6.11  
*   Minor internal refactoring  

#### 0.4.3

*   Updated terraform model data to match Terraform 0.6.8  

#### 0.4.2

*   Fix false 'unknown block' inspection result (issue #12)  
*   Fix compatibility with non IDEA IDEs (issue #13)  

#### 0.4.1

*   Fix ClassCastException (issue #11)  

#### 0.4.0

*   Autocompletion in interpolations  
*   Go to definition from interpolations to provider/resources properties, etc.  
*   Find usages of provider/resources properties, etc. in interpolations  
*   Inplace properties renaming (with usages tracking)  

#### 0.3.5

*   Updated terraform model data to match Terraform 0.6.5  
*   Added 'unknown block type' inspection.  

#### 0.3.4

*   Improvements in completion and 'required property is missing' inspection.  

#### 0.3.3

*   Improvements in completion. Now it available for almost all blocks properties (resource, provider, variable, etc)  
*   Missing required properties inspection improved.  

#### 0.3.2

*   Enable resource type and properties completion introduced in 0.3.0  

#### 0.3.1

*   Register '.nomad' (Nomad) and 'Appfile' (Otto) files as HCL files  

#### 0.3.0

*   Added Inspection for missing required properties in providers.  

#### 0.2.8, 0.2.9

*   Another minor improvement in code formatter around caret placement after pressing `Enter`  

#### 0.2.7

*   Code formatter improved  

#### 0.2.6

*   Support heredoc values (issue #8)  

#### 0.2.5

*   Compatibility with 139.X platform branch  

#### 0.2.4

*   Improvements around incorrect files  
*   Now numbers like '10Gb' properly handled  

#### 0.2.3

*   Fixes issue #6: Double quote handled incorrectly in interpolation  

#### 0.2.2

*   Fixes around Auto-create closing quotes, braces, brackets (issue #4)  
*   Improve interpolation language injector  

#### 0.2.1

*   Auto-create closing quotes, braces, brackets (issue #4)  
*   Highlight matching bracket signs (issue #5)  

#### 0.2.0

*   Terraform Interpolation language support (automatically injected in .tf files)  

#### 0.1.4

*   Fix grammar: identifiers may contain numbers  

#### 0.1.3

*   Register for '.tf' files extension  
*   Minor improvements  

#### 0.1.2

*   Comment/Uncomment line action  
*   'Structure' tool window support  
*   Objects and arrays folding in editor  

#### 0.1.1

*   Recompiled for Java 6  

#### 0.1

*   Syntax highlighting