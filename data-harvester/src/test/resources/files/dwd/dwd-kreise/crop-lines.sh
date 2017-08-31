#!/bin/bash

LINE_RANGE="1,10p"
IGNORE_FILES=(
    # represents districts
    "ebe26100-ef92-4b84-8e9e-7ce3a1f7d44e.csv"
)

# TODO 
# make ignores to array: -not \( -path './etc/*' -o -path './cache/*' -o -path './archive/*'  \)

add_excludes() {
    declare -a ignore_files=("${!1}")
    if [ ${ignore_files[@]} -eq 0 ]; then
        return "";
    else
        ignore=
        for i in "${ignore_files[@]}"
        do
           echo $i
        done
        return "$ignore \)"
    fi
}

# inplace cropping lines of given range within CSV files in current folder
for file in `find . -type f -name '*.csv -not "$IGNORE_FILES[0]"'` ; do sed -i -n $LINE_RANGE "$file" ; done

#for file in `find . -type f -name '*.csv add_excludes IGNORE_FILES[@]'` ; do sed -i -n $LINE_RANGE "$file" ; done

