
import java.io.FileReader;
import java.util.Iterator;
import java.util.StringJoiner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class QueryBuilder {

	public static String selectQry = "SELECT $(columns)  FROM $(tablename) $(join)  $(condition) ";

	public static void main(String[] args) {
		JSONParser parser = new JSONParser();
		try {
			
			Object obj = parser.parse(new FileReader("pathToJsonFile"));//provide valid path for json file EX: "C:/Users/Desktop/test.json"
			JSONObject jsonObject = (JSONObject) obj;

			QueryBuilder queryBuilder = new QueryBuilder();
			String buildSelectQry = queryBuilder.buildSelectQry(jsonObject);

			System.out.println("select qry:" + buildSelectQry);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String buildSelectQry(JSONObject jsonObject) {

		String baseTable = (String) jsonObject.get("baseTable");
		String alias = (String) jsonObject.get("alias");
		String baseColumns = (String) jsonObject.get("baseColumns");
		JSONArray columnsConditions = (JSONArray) jsonObject.get("columnsConditions");

		try {
			StringJoiner columnJoiner = new StringJoiner(",");
			selectQry = selectQry.replace("$(tablename)", baseTable + " " + alias);

			// column conditions
			Iterator iterator = columnsConditions.iterator();
			StringJoiner columnConditionJoiner = new StringJoiner(" AND ");
			while (iterator.hasNext()) {
				JSONObject colConditionObject = (JSONObject) iterator.next();
				columnConditionJoiner.add(buildCondition(colConditionObject));
			}
			selectQry = selectQry.replace("$(condition)",
					columnConditionJoiner.toString().isEmpty() ? "" : " where " + columnConditionJoiner.toString());

			// join tables
			JSONArray joinTables = (JSONArray) jsonObject.get("joinTables");
			Iterator joinIterator = joinTables.iterator();
			StringJoiner joiner = new StringJoiner("  ");
			while (joinIterator.hasNext()) {
				JSONObject joinObject = (JSONObject) joinIterator.next();
				joiner.add(buildJoinQry(joinObject, alias));
				columnJoiner
						.add(buildColumns(joinObject.get("columns").toString(), joinObject.get("alias").toString()));
			}
			selectQry = selectQry.replace("$(join)", joiner.toString());

			// build base columns
			columnJoiner.add(buildColumns(baseColumns, alias));
			Boolean distinctColumns = (Boolean) jsonObject.get("distinctColumns");

			selectQry = selectQry.replace("$(columns)", (distinctColumns ?  " distinct "+columnJoiner.toString() : columnJoiner.toString()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return selectQry;
	}

	public String buildColumns(String columns, String alias) {
		StringJoiner columnJoiner = new StringJoiner(",");
		if (columns != null && !columns.isEmpty()) {
			String[] columnArr = columns.split(",");
			for (String column : columnArr) {
				columnJoiner.add(alias + "." + column.trim());
			}
		}
		return columnJoiner.toString();
	}

	public String buildCondition(JSONObject jsonObject) {
		String operator = (String) jsonObject.get("operator");
		String alias = (String) jsonObject.get("alias");
		String fieldName = (String) jsonObject.get("fieldName");
		Object fieldValue = (Object) jsonObject.get("fieldValue");
		fieldName = alias + "." + fieldName;

		String conditonStr = null;
		if (operator.equalsIgnoreCase("lessthaneq")) {
			conditonStr = fieldName + " <= " + fieldValue;
		} else if (operator.equalsIgnoreCase("greaterthaneq")) {
			conditonStr = fieldName + " >= " + fieldValue;
		} else if (operator.equalsIgnoreCase("lessthan")) {
			conditonStr = fieldName + " < " + fieldValue;
		} else if (operator.equalsIgnoreCase("greaterthan")) {
			conditonStr = fieldName + " > " + fieldValue;
		} else if (operator.equalsIgnoreCase("equal")) {
			if (fieldValue instanceof Number) {
				conditonStr = fieldName + " = " + fieldValue;
			} else {
				conditonStr = fieldName + " = '" + fieldValue + "'";
			}
		} else if (operator.equalsIgnoreCase("in")) {
			conditonStr = fieldName + " in ( '" + fieldValue + "' )";
		} else if (operator.equalsIgnoreCase("contains")) {
			conditonStr = fieldName + " like '%" + fieldValue + "%' ";
		} else if (operator.equalsIgnoreCase("startswith")) {
			conditonStr = fieldName + " like '%" + fieldValue + "' ";
		} else if (operator.equalsIgnoreCase("endswith")) {
			conditonStr = fieldName + " like '" + fieldValue + "%' ";
		} else if (operator.equalsIgnoreCase("notequal")) {
			if (fieldValue instanceof Number) {
				conditonStr = fieldName + " <>  " + fieldValue;
			} else {
				conditonStr = fieldName + " <>  '" + fieldValue + "'";
			}
		} else if (operator.equalsIgnoreCase("between")) {
			Object fieldValue2 = (Object) jsonObject.get("fieldValue2");
			conditonStr = fieldName + " BETWEEN " + fieldValue + " AND "+ fieldValue2;
		}

		return conditonStr;
	}

	public String buildJoinQry(JSONObject jsonObject, String alias) {

		String table = (String) jsonObject.get("table");
		String joinType = (String) jsonObject.get("joinType");
		String column = (String) jsonObject.get("column");
		String joinColumn = (String) jsonObject.get("joinColumn");
		String alias1 = (String) jsonObject.get("alias");

		String joinTypeStr = "$(joinType) $(table) ON $(joinCondition)";

		joinTypeStr = joinTypeStr.replace("$(joinType)", joinType + " join");
		joinTypeStr = joinTypeStr.replace("$(table)", table + " " + alias1);
		joinTypeStr = joinTypeStr.replace("$(joinCondition)", alias + "." + joinColumn + "=" + alias1 + "." + column);
		return joinTypeStr;
	}

}
