# Contributing to htmlparser

## Adding new elements

When adding new elements to the parser, you must regenerate the element name hash tables in `src/nu/validator/htmlparser/impl/ElementName.java`.

### Step 1: Add the new element constant

Add a new `static final ElementName` constant for your element, following the existing pattern:

```java
public static final ElementName MYNEWELEMENT = new ElementName(
    "mynewelement", "mynewelement",
    // CPPONLY: NS_NewHTMLElement,
    // CPPONLY: NS_NewSVGUnknownElement,
    TreeBuilder.OTHER);
```

The flags (like `TreeBuilder.OTHER`, `SPECIAL`, `SCOPING`, etc.) depend on how the element should be handled by the tree builder.

### Step 2: Uncomment the code generation sections

Uncomment three sections in `ElementName.java`:

1. **The imports** near the top (~lines 26-39):
   - `java.io.*`
   - `java.util.*`
   - `java.util.regex.*`

2. **`implements Comparable<ElementName>`** on the class declaration (~line 49)

3. **The code generation block** marked with:
   `"START CODE ONLY USED FOR GENERATING CODE uncomment and run to regenerate"`
   That includes the `main()` method and helper functions (~lines 272-659)

### Step 3: Add case to treeBuilderGroupToName() if needed

If your element uses a new `TreeBuilder` group constant, add a case for it in the `treeBuilderGroupToName()` method within the code generation block.

### Step 4: Compile and run

Compile the project:

```bash
mvn compile
```

Run the `ElementName` class with paths to the Gecko tag-list files:

```bash
java -cp target/classes nu.validator.htmlparser.impl.ElementName \
    /path/to/nsHTMLTagList.h \
    /path/to/SVGTagList.h
```

**For Java-only builds** (not Gecko), you can use empty dummy files:

```bash
mkdir -p /tmp/tagfiles
touch /tmp/tagfiles/nsHTMLTagList.h /tmp/tagfiles/SVGTagList.h
java -cp target/classes nu.validator.htmlparser.impl.ElementName \
    /tmp/tagfiles/nsHTMLTagList.h \
    /tmp/tagfiles/SVGTagList.h
```

> [!NOTE]
> Using empty files means the `CPPONLY` comments will all show `NS_NewHTMLUnknownElement`. For Gecko builds, use the actual files from moz-central:
> - `parser/htmlparser/nsHTMLTagList.h`
> - `dom/svg/SVGTagList.h`

### Step 5: Update the generated arrays

The program outputs:
1. All element constant definitions (with updated `CPPONLY` comments if using real Gecko tag files)
2. The `ELEMENT_NAMES` array in level-order binary search tree order
3. The `ELEMENT_HASHES` array with corresponding hash values

Replace the existing `ELEMENT_NAMES` and `ELEMENT_HASHES` arrays in the file with the generated output. The arrays must stay in sync—element at position N in `ELEMENT_NAMES` must have its hash at position N in `ELEMENT_HASHES`.

### Step 6: Re-comment the code generation sections

After regeneration, comment out the sections you uncommented in Step 2 to restore the file to its normal state.

### Step 7: Run tests

Verify your changes work correctly:

```bash
mvn test
```

### Technical Details

The hash function (`bufToHash`) creates a unique integer for each element name using the element's length and specific character positions. The arrays are organized as a level-order binary search tree for O(log n) lookup performance.

If you encounter a hash collision (two elements with the same hash), the regeneration will report an error. That would require modifying the hash function, which has not been necessary historically.
