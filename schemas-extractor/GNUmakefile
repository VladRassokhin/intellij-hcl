default: build

build: schemas

schemas: providers.list
	@sh -c "'$(CURDIR)/build-all.sh'"

providers.list:
	@sh -c "'$(CURDIR)/fetch-providers.sh'"


.PHONY: build
