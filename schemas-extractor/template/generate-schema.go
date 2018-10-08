package main

import (
	"github.com/hashicorp/terraform/helper/schema"
	tf "github.com/hashicorp/terraform/terraform"
	"github.com/terraform-providers/__FULL_NAME__/__NAME__"

	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"
)

// ExportSchema should be called to export the structure
// of the provider.
func Export(p *schema.Provider) *ResourceProviderSchema {
	result := new(ResourceProviderSchema)

	result.Name = "__NAME__"
	result.Type = "provider"
	result.Version = "__REVISION__"
	result.Provider = schemaMap(p.Schema).Export()
	result.Resources = make(map[string]SchemaInfoWithTimeouts)
	result.DataSources = make(map[string]SchemaInfoWithTimeouts)

	for k, r := range p.ResourcesMap {
		result.Resources[k] = ExportResourceWithTimeouts(r)
	}
	for k, ds := range p.DataSourcesMap {
		result.DataSources[k] = ExportResourceWithTimeouts(ds)
	}

	return result
}

const (
	TimeoutCreate  = "create"
	TimeoutRead    = "read"
	TimeoutUpdate  = "update"
	TimeoutDelete  = "delete"
	TimeoutDefault = "default"
)

func timeoutKeys() []string {
	return []string{
		TimeoutCreate,
		TimeoutRead,
		TimeoutUpdate,
		TimeoutDelete,
		TimeoutDefault,
	}
}

func ExportResourceWithTimeouts(r *schema.Resource) SchemaInfoWithTimeouts {
	var timeouts []string
	t := r.Timeouts
	if t != nil {
		for _, key := range timeoutKeys() {
			var timeout *time.Duration
			switch key {
			case TimeoutCreate:
				timeout = t.Create
			case TimeoutUpdate:
				timeout = t.Update
			case TimeoutRead:
				timeout = t.Read
			case TimeoutDelete:
				timeout = t.Delete
			case TimeoutDefault:
				timeout = t.Default
			default:
				panic("Unsupported timeout key, update switch statement!")
			}
			if timeout != nil {
				timeouts = append(timeouts, key)
			}
		}
	}
	result := make(SchemaInfoWithTimeouts)
	for nk, nv := range ExportResource(r) {
		result[nk] = nv
	}
	if len(timeouts) > 0 {
		result["__timeouts__"] = timeouts
	}
	return result
}

func ExportResource(r *schema.Resource) SchemaInfo {
	return schemaMap(r.Schema).Export()
}

// schemaMap is a wrapper that adds nice functions on top of schemas.
type schemaMap map[string]*schema.Schema

// Export exports the format of this schema.
func (m schemaMap) Export() SchemaInfo {
	result := make(SchemaInfo)
	for k, v := range m {
		item := export(v)
		result[k] = item
	}
	return result
}

func export(v *schema.Schema) SchemaDefinition {
	item := SchemaDefinition{}

	item.Type = shortenType(fmt.Sprintf("%s", v.Type))
	item.Optional = v.Optional
	item.Required = v.Required
	item.Description = v.Description
	item.InputDefault = v.InputDefault
	item.Computed = v.Computed
	item.MaxItems = v.MaxItems
	item.MinItems = v.MinItems
	item.PromoteSingle = v.PromoteSingle
	item.ComputedWhen = v.ComputedWhen
	item.ConflictsWith = v.ConflictsWith
	item.Deprecated = v.Deprecated
	item.Removed = v.Removed

	if v.Elem != nil {
		item.Elem = exportValue(v.Elem, fmt.Sprintf("%T", v.Elem))
	}

	// TODO: Find better solution
	if defValue, err := v.DefaultValue(); err == nil && defValue != nil {
		defValueAsMap, ok := defValue.(map[string]interface{})
		if !ok || len(defValueAsMap) != 0 {
			item.Default = exportValue(defValue, fmt.Sprintf("%T", defValue))
		}
	}
	return item
}

func shortenType(value string) string {
  if (len(value) > 4 && value[0:4] == "Type") {
    return value[4:]
  }
  return value
}

func exportValue(value interface{}, t string) *SchemaElement {
	s2, ok := value.(*schema.Schema)
	if ok {
		return &SchemaElement{Type: "SchemaElements", ElementsType: shortenType(fmt.Sprintf("%s", s2.Type))}
	}
	r2, ok := value.(*schema.Resource)
	if ok {
		return &SchemaElement{Type: "SchemaInfo", Info: ExportResource(r2)}
	}
	vt, ok := value.(schema.ValueType)
	if ok {
	  return &SchemaElement{Value: shortenType(fmt.Sprintf("%v", vt))}
	}
	// Unknown case
	return &SchemaElement{Type: t, Value: fmt.Sprintf("%v", value)}
}

func Generate(provider *schema.Provider, name string, outputPath string) {
	outputFilePath := filepath.Join(outputPath, fmt.Sprintf("%s.json", name))

	if err := DoGenerate(provider, name, outputFilePath); err != nil {
		fmt.Fprintln(os.Stderr, "Error: ", err.Error())
		os.Exit(255)
	}
}

func DoGenerate(provider *schema.Provider, providerName string, outputFilePath string) error {
	providerJson, err := json.MarshalIndent(Export(provider), "", "  ")

	if err != nil {
		return err
	}

	file, err := os.Create(outputFilePath)
	if err != nil {
		return err
	}

	defer file.Close()

	_, err = file.Write(providerJson)
	if err != nil {
		return err
	}

	return file.Sync()
}

type SchemaElement struct {
	// One of "schema.ValueType" or "SchemaElements" or "SchemaInfo"
	Type string `json:",omitempty"`
	// Set for simple types (from ValueType)
	Value string `json:",omitempty"`
	// Set if Type == "SchemaElements"
	ElementsType string `json:",omitempty"`
	// Set if Type == "SchemaInfo"
	Info SchemaInfo `json:",omitempty"`
}

type SchemaDefinition struct {
	Type          string `json:",omitempty"`
	Optional      bool   `json:",omitempty"`
	Required      bool   `json:",omitempty"`
	Description   string `json:",omitempty"`
	InputDefault  string `json:",omitempty"`
	Computed      bool   `json:",omitempty"`
	MaxItems      int    `json:",omitempty"`
	MinItems      int    `json:",omitempty"`
	PromoteSingle bool   `json:",omitempty"`

	ComputedWhen  []string `json:",omitempty"`
	ConflictsWith []string `json:",omitempty"`

	Deprecated string `json:",omitempty"`
	Removed    string `json:",omitempty"`

	Default *SchemaElement `json:",omitempty"`
	Elem    *SchemaElement `json:",omitempty"`
}

type SchemaInfo map[string]SchemaDefinition
type SchemaInfoWithTimeouts map[string]interface{}
//{
//	SchemaInfo `json:""`
//	Timeouts []string `json:"__timeouts__,omitempty"`
//}

// ResourceProviderSchema
type ResourceProviderSchema struct {
	Name        string                `json:"name"`
	Type        string                `json:"type"`
	Version     string                `json:"version"`
	Provider    SchemaInfo            `json:"provider"`
	Resources   map[string]SchemaInfoWithTimeouts `json:"resources"`
	DataSources map[string]SchemaInfoWithTimeouts `json:"data-sources"`
}

func main() {
	var provider tf.ResourceProvider
	provider = __NAME__.Provider()
	Generate(provider.(*schema.Provider), "__NAME__", "__OUT__")
}
