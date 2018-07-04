default: build

clean:
	rm -f providers.list.full providers.list

build: schemas

schemas: providers.list.full
	@sh -c "'$(CURDIR)/build-all.sh'"

providers.list.full:
	@sh -c "'$(CURDIR)/fetch-providers.sh'"


.PHONY: build
