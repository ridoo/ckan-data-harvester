#!/bin/bash

LINE_RANGE="1,10p"
declare -a IGNORE_FILES=(
    # represents districts
    "./ebe26100-ef92-4b84-8e9e-7ce3a1f7d44e.csv"
    "./546add17-3bcc-46a8-a550-a9e2e07a4648.csv"
)

# TODO 
# make ignores to array: -not \( -path './etc/*' -o -path './cache/*' -o -path './archive/*'  \)

function add_excludes {
    declare -a items=("$@")
    
    if [ "${#items[*]}" -eq 0 ]; then
        echo ""
    fi

    local excludes="-not \( -path \"${items[0]}\""
    if [ "${#items[*]}" -gt 1 ]; then
        for i in "${items[@]:1}"
        do
           excludes="$excludes -o -path \"${i}\""
        done
    fi

    echo "$excludes \)"
}

# inplace cropping lines of given range within CSV files in current folder
exclude_files=$(add_excludes "${IGNORE_FILES[@]}")
echo "$exclude_files"
echo find . -type f -name \"*.csv\" "$exclude_files" -exec sed -i -n \"$LINE_RANGE\" {} \\\;
find . -type f -name \"*.csv\" "$exclude_files" -exec sed -i -n \"$LINE_RANGE\" {} \\\;
#find . -name "*.csv" -not \( -path "ebe*" -o -path "546add*" \) -exec sed -i -n "1,10p" {} \;


#for file in `find . -type f -name '*.csv add_excludes IGNORE_FILES[@]'` ; do sed -i -n $LINE_RANGE "$file" ; done

