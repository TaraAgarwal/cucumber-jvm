package cucumber.api;

import cucumber.runtime.ParameterInfo;
import cucumber.runtime.table.DiffableRow;
import cucumber.runtime.table.TableConverter;
import cucumber.runtime.table.TableDiffException;
import cucumber.runtime.table.TableDiffer;
import cucumber.runtime.table.TypeReference;
import cucumber.runtime.xstream.LocalizedXStreams;
import gherkin.formatter.PrettyFormatter;
import gherkin.formatter.model.DataTableRow;
import gherkin.formatter.model.Row;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the data from a <a href="http://cucumber.info/gherkin.html#data-tables">Gherkin DataTable</a>. Cucumber will convert the table in Gherkin
 * to a DataTable instance and pass it to a step definition.
 */
public class DataTable {

    private final List<List<String>> raw;
    private final List<DataTableRow> gherkinRows;
    private final TableConverter tableConverter;

    public static DataTable create(List<?> raw) {
        return create(raw, Locale.getDefault(), null, new String[0]);
    }

    public static DataTable create(List<?> raw, String format, String... columnNames) {
        return create(raw, Locale.getDefault(), format, columnNames);
    }

    public static DataTable create(List<?> raw, Locale locale, String... columnNames) {
        return create(raw, locale, null, columnNames);
    }

    private static DataTable create(List<?> raw, Locale locale, String format, String... columnNames) {
        ParameterInfo parameterInfo = new ParameterInfo(null, format, null, null);
        TableConverter tableConverter = new TableConverter(new LocalizedXStreams(Thread.currentThread().getContextClassLoader()).get(locale), parameterInfo);
        return tableConverter.toTable(raw, columnNames);
    }

    /**
     * Creates a new DataTable. This constructor should not be called by Cucumber users - it's used internally only.
     *
     * @param gherkinRows    the underlying rows.
     * @param tableConverter how to convert the rows.
     */
    public DataTable(List<DataTableRow> gherkinRows, TableConverter tableConverter) {
        this.gherkinRows = gherkinRows;
        this.tableConverter = tableConverter;
        List<List<String>> raw = new ArrayList<List<String>>();
        for (Row row : gherkinRows) {
            List<String> list = new ArrayList<String>();
            list.addAll(row.getCells());
            raw.add(Collections.unmodifiableList(list));
        }
        this.raw = Collections.unmodifiableList(raw);
    }

    /**
     * Converts the table to a 2D array.
     *
     * @return a List of List of String.
     */
    public List<List<String>> raw() {
        return this.raw;
    }

    public <T> T convert(Type type) {
        return tableConverter.convert(type, this);
    }

    /**
     * Converts the table to a List of Map. The top row is used as keys in the maps,
     * and the rows below are used as values.
     *
     * @return a List of Map.
     */
    public List<Map<String, String>> asMaps() {
        return asList(new TypeReference<Map<String, String>>() {
        }.getType());
    }

    /**
     * Converts the table to a List of objects. The top row is used to identifies the fields/properties
     * of the objects.
     * <p/>
     * Backends that support generic types can declare a parameter as a List of a type, and Cucumber will
     * do the conversion automatically.
     *
     * @param type the type of the result (should be a {@link List} generic type)
     * @param <T>  the type of each object
     * @return a list of objects
     */
    public <T> List<T> asList(Type type) {
        List<T> result = tableConverter.toList(type, this);
        return result;
    }

    public List<String> topCells() {
        return raw.get(0);
    }

    public List<List<String>> cells(int firstRow) {
        return raw.subList(firstRow, raw.size());
    }

    /**
     * Creates another table using the same {@link Locale} and {@link Format} that was used to create this table.
     *
     * @param raw         a list of objects
     * @param columnNames optional explicit header columns
     * @return a new table
     */
    public DataTable toTable(List<?> raw, String... columnNames) {
        return tableConverter.toTable(raw, columnNames);
    }

    /**
     * Diffs this table with {@code other}, which can be a {@code List&lt;List&lt;String&gt;&gt;} or a
     * {@code List&lt;YourType&gt;}.
     *
     * @param other the other table to diff with.
     * @throws cucumber.runtime.table.TableDiffException
     *          if the tables are different.
     */
    public void diff(List<?> other) throws TableDiffException {
        List<String> topCells = topCells();
        DataTable otherTable = toTable(other, topCells.toArray(new String[topCells.size()]));
        diff(otherTable);
    }

    /**
     * Diffs this table with {@code other}.
     *
     * @param other the other table to diff with.
     * @throws TableDiffException if the tables are different.
     */
    public void diff(DataTable other) throws TableDiffException {
        new TableDiffer(this, other).calculateDiffs();
    }

    /**
     * Internal method. Do not use.
     *
     * @return a list of raw rows.
     */
    public List<DataTableRow> getGherkinRows() {
        return Collections.unmodifiableList(gherkinRows);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        PrettyFormatter pf = new PrettyFormatter(result, true, false);
        pf.table(getGherkinRows());
        pf.eof();
        return result.toString();
    }

    public List<DiffableRow> diffableRows() {
        List<DiffableRow> result = new ArrayList<DiffableRow>();
        List<List<String>> convertedRows = raw();
        for (int i = 0; i < convertedRows.size(); i++) {
            result.add(new DiffableRow(getGherkinRows().get(i), convertedRows.get(i)));
        }
        return result;
    }

    public TableConverter getTableConverter() {
        return tableConverter;
    }

    public List<String> flatten() {
        List<String> result = new ArrayList<String>();
        for (List<String> rows : raw()) {
            for (String cell : rows) {
                result.add(cell);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataTable)) return false;

        DataTable dataTable = (DataTable) o;

        if (!raw.equals(dataTable.raw)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }
}
