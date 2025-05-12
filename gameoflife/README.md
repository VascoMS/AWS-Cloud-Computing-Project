## Game of Life

This project contains Conway's Game of Life functionality.

### How to build

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

### How to run locally

To run Game of Life locally in CLI, execute this command:

```
java -cp target/gameoflife-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.gameoflife.GameOfLifeHandler <map-json-filename> [<iterations>]
```

The JSON file should have the structure as in the example below:

```json
{
  "map": [
    [0, 0, 1, 0, 0, 0, 0, 0, 0, 0],
    [1, 0, 1, 0, 0, 0, 0, 0, 0, 0],
    [0, 1, 1, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
  ],
  "iterations" : 200
}
```

In case the second argument (`iterations`) is provided, it will override the value from the JSON file. It should be a valid integer value. This argument is optional.
