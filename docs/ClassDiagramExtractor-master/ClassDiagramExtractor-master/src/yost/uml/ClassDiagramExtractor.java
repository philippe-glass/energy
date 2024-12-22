package yost.uml;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution procedure:
 * - configure settings object
 * - launch the this executable class
 * - copy the content of the generated .gv file (test.gv) and paste it to https://dreampuf.github.io/GraphvizOnline
 * - download the generated svg file and display it on a web browser
 */

public class ClassDiagramExtractor {
	private static ExtractorSettings settings = null;
	private static List<ClassInfo> listClasses = new ArrayList<ClassInfo>();
	private static Map<String, String> mapPackageColor = new HashMap<String, String>();
	private static ClassLoader classLoader;
	private final static int DIAG_ENERGY_OBJECT = 1;
	private final static int DIAG_ENERGY_AGENT= 2;
	private final static int DIAG_PREDICTION = 3;
	private final static int DIAG_LEARNING_MODEL = 4;
	private final static int DIAG_PROSUMER = 4;
	private final static int DIAG_LIGHT_SERVER = 11;
	private final static int DIAG_MDW_ECOLAWS = 101;
	private final static int DIAG_MDW_AGENTS = 102;
	private final static int DIAG_TEST_SIMULATORS = 201;
	static {
		// common settings
		String root = "C:\\projects\\smartgrids\\energy2\\";
		settings = new ExtractorSettings();
		settings.setClassFolders(Arrays.asList(
			  root + "SapereMiddlewareStandalone\\bin"
			, root + "sapereapi\\target\\classes"
			, root + "saperetest\\target\\classes")
				);
		settings.setGraphvizFilename(root + "class_diagram.gv");
		settings.setPackageColors(Arrays.asList("#000000", "#696969", "#808080", "#A9A9A9", "#C0C0C0"));
		int chosenDiagram = DIAG_LIGHT_SERVER;

		//settings.setFilterPackage(Arrays.asList("com.sapereapi", "eu.sapere.middleware"));
		//settings.setFilterPackage(Arrays.asList("com.sapereapi.model.learning"));
		// Diagram for energy objects
		if (chosenDiagram == DIAG_ENERGY_OBJECT) {
			settings.setFilterPackage(Arrays.asList("com.sapereapi.model.energy"
					, "com.sapereapi.model.energy.warning"
					//, "com.sapereapi.model.energy.award"
					//, "com.sapereapi.model.energy.reschedule"
					));
			settings.setFilterPackageStrict(true);
			settings.setExcludedClasses(
					Arrays.asList("_RescheduleItem", "_RescheduleTable", "AgentForm", "EnergyStorage", "ExtraSupply"
							, ".Device", "ProsumerItem", "ReducedContract", "ExtendedEnergyEvent"));
		}
		// Diagram for energy agents
		if (chosenDiagram == DIAG_ENERGY_AGENT) {
			// Diagram for energy agents
			settings.setFilterPackage(Arrays.asList("com.sapereapi.agent.energy", "eu.sapere.middleware.agent"));
			settings.setFilterPackageStrict(true);
			settings.setExcludedClasses(
					Arrays.asList("OldProducer", "OldConsumer", "AgentLearningData", "BasicSapereAgent"));
		}
		if(chosenDiagram == DIAG_LIGHT_SERVER) {
			// Diagram for energy agents
			settings.setFilterPackage(Arrays.asList("com.sapereapi.lightserver", "com.sapereapi.model"));
			settings.setFilterPackageStrict(true);
			settings.setExcludedClasses(
					Arrays.asList("QueryHandler", "FooHandler", "ServiceHandler", "Session", "LsaForm", "Sapere", "NodeContext"));

		}
		// Diagram for eco-laws
		if (chosenDiagram == DIAG_MDW_ECOLAWS) {
			settings.setFilterPackage(Arrays.asList("eu.sapere.middleware.node.lsaspace.ecolaws"));
		}
		// Diagram for agent middleware classes
		if (chosenDiagram == DIAG_MDW_AGENTS) {
			settings.setFilterPackage(Arrays.asList("eu.sapere.middleware.agent"));
			settings.setExcludedClasses(Arrays.asList("AgentLearningData", "BasicSapereAgent"));
		}
		// Diagram for predictionData class
		if (chosenDiagram == DIAG_PREDICTION) {
			settings.setFilterPackage(Arrays.asList("com.sapereapi.model.learning.prediction"));
			settings.setExcludedClasses(
				Arrays.asList("MultiPredictionsData", "Statistic", "Correction", "Deviation", "Aggregation"));
		}
		// Diagram for learning model classes
		if (chosenDiagram == DIAG_LEARNING_MODEL) {
			settings.setFilterPackage(Arrays.asList("com.sapereapi.model.learning"));
			settings.setExcludedClasses(
				Arrays.asList(".learning.prediction", ".learning.aggregation", ".lstm.request", "Compact", "Tracking"
					, "BooleanOperator", "NodeStates", "FeaturesKey"));
		}
		// Diagram for prosumer class
		if (chosenDiagram == DIAG_PROSUMER) {
			settings.setFilterPackage(Arrays.asList("com.sapereapi.agent.energy.manager"
					,"com.sapereapi.agent.energy.ProsumerAgent"));
			settings.setExcludedClasses(
				Arrays.asList(".learning.prediction"));
		}
		// Diagram for tester simulators
		if (chosenDiagram == DIAG_TEST_SIMULATORS) {
			settings.setFilterPackage(Arrays.asList("com.saperetest"));
			settings.setExcludedClasses(Arrays.asList("DBHelper", "ClusterTotal"));
		}
	}

	public ClassDiagramExtractor() {
		loadClasses();
		String content = createDiagram();
		System.out.println(content);
		ExtractorUtil.writeFile(content, settings.getGraphvizFilename());
	}

	private void loadClasses() {
		List<URL> urls = new ArrayList<URL>();
		// preapare class loader with the retreived URLs
		for (String classFolder : settings.getClassFolders()) {
			File classFolderFile = new File(classFolder);
			try {
				urls.add(classFolderFile.toURI().toURL());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		classLoader = new URLClassLoader(urls.toArray(new URL[0]));

		// retreive class information
		listClasses = new ArrayList<ClassInfo>();
		for (String classFolder : settings.getClassFolders()) {
			File classFolderFile = new File(classFolder);
			listClasses.addAll(scanFolder(classFolder, classFolderFile));
		}
	}


	private String loadSuperClassName(Class<?> classContent) {
		//String className = classContent.getName();
		Class<?> superClass = classContent.getSuperclass();
		if (superClass != null) {
			String superclassName = superClass.getName();
			if(!"java.lang.Object".equals(superclassName)) {
				//System.out.println("getInheritances : for debug : class= " + className + ", superClass = " + superClass);
				if(settings.matchesFilterPackage(superclassName)) {
					return superclassName;
				}
			}
		}
		return null;
	}

	private List<String> loadInterfaces(Class<?> classContent) {
		List<String> interfaceNames = new ArrayList<String>();
		Class<?>[] interfaces = classContent.getInterfaces();
		for (Class<?> interface0 : interfaces) {
			String interfaceName = interface0.getName();
			if(settings.matchesFilterPackage(interfaceName)) {
				interfaceNames.add(interfaceName);
			}
		}
		return interfaceNames;
	}

	private Map<String, List<String>> auxLoadParameterizedTypes(Field field, Type genericType) {
		Map<String, List<String>> properties = new HashMap<String, List<String>>();
		if (genericType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) genericType;
			for (Type type : pt.getActualTypeArguments()) {
				// System.out.println("loadProperties " + className + " " + field.getName() + "
				// : " + type.toString());
				String className2 = type.getTypeName();
				if (type instanceof ParameterizedType) {
					properties = ExtractorUtil.auxAddProperties(properties, auxLoadParameterizedTypes(field, type));
				} else if (settings.matchesFilterPackage(className2)) {
					properties = ExtractorUtil.auxAddEntry(properties, className2, field.getName());
				}
			}
		}
		return properties;
	}

	private Map<String, List<String>> loadProperties(Class<?> classContent) {
		String className = classContent.getName();
		Map<String, List<String>> properties = new HashMap<String, List<String>>();
		try {
			Field[] fields = classContent.getDeclaredFields();
			for (Field field : fields) {
				String fieldClassName = field.getType().getName().replace("$", ".");
				Type genericType = field.getGenericType();
				if (field.getGenericType() instanceof ParameterizedType) {
					properties =  ExtractorUtil.auxAddProperties(properties, auxLoadParameterizedTypes(field, genericType));
					//properties.putAll(auxLoadParameterizedTypes(field, genericType));
				}
				if (fieldClassName.contains("[")) {
					fieldClassName = field.getType().getComponentType().getName();
				}
				boolean toExclude = false;
				if(classContent.isEnum() && className.equals(classContent.getName())) {
					System.out.println("loadProperties enum " + className + " to exclude " + field.getName() );
					toExclude = true;
				}
				if (!toExclude && settings.matchesFilterPackage(fieldClassName)) {
					if(!properties.containsKey(fieldClassName)) {
						properties.put(fieldClassName, new ArrayList<String>());
					}
					List<String> listProps = properties.get(fieldClassName);
					if(!listProps.contains(field.getName())) {
						listProps.add(field.getName());
					}
				}
			}
		} catch (Throwable e) {
			System.out.println("### loadProperties " + classContent.getName() + " " + e);
		}
		return properties;
	}

	private List<ClassInfo> scanFolder(String rootPath, File folder) {
		List<ClassInfo> files = new ArrayList<ClassInfo>();
		String filepath, classCompleteName, classSimpleName;
		for (File file : folder.listFiles()) {
			if (file.isFile()) {
				filepath = file.getPath();
				if (filepath.endsWith(".class")) {
					classCompleteName = ExtractorUtil.getClassCompleteName(rootPath, filepath);
					if (settings.matchesFilterPackage(classCompleteName)) {
						classSimpleName = ExtractorUtil.getClassSimpleName(classCompleteName);
						if (!ExtractorUtil.isNumeric(classSimpleName)) {
							int idx = classCompleteName.indexOf(classSimpleName);
							String packageName = classCompleteName.substring(0, idx - 1);
							ClassInfo classInfo = new ClassInfo();
							classInfo.setPackageName(packageName);
							classInfo.setSimpleName(classSimpleName);
							classInfo.setRootPath(rootPath);
							// Set color
							if (!mapPackageColor.containsKey(packageName)) {
								int packageIndex = mapPackageColor.size();
								String color = settings.getPackageColor(packageIndex);
								if (color == null) {
									color = "000000";
								}
								mapPackageColor.put(packageName, color);
							}
							String packageColor = mapPackageColor.get(packageName);
							classInfo.setPackageColor(packageColor);
							Class<?> classContent;
							try {
								classContent = classLoader.loadClass(classCompleteName);
								// interface
								classInfo.setInterface(classContent.isInterface());
								// abstract
								classInfo.setAbstract(Modifier.isAbstract(classContent.getModifiers()));
								// deprecated
								Deprecated[] deprecatedAnnotations = classContent.getAnnotationsByType(Deprecated.class);
								classInfo.setEnum(classContent.isEnum());
								classInfo.setDeprecated(deprecatedAnnotations.length != 0);
								// super class
								String superClassName = loadSuperClassName(classContent);
								if (superClassName != null) {
									classInfo.setSuperClassName(superClassName);
								}
								classInfo.setInterfaces(loadInterfaces(classContent));
								if ("ConfirmationTable".equals(classSimpleName)) {
									System.out.println("scanFolder " + classSimpleName + " for debug");
								}
								classInfo.setProperties(loadProperties(classContent));
							} catch (ClassNotFoundException | NoClassDefFoundError e) {
								System.out.println("### scanFolder " + classCompleteName + " " + e.toString());
							}
							files.add(classInfo);
						}
					}
				}
			} else {
				files.addAll(scanFolder(rootPath, file)); // recursive
			}
		}
		return files;
	}


	/*
	 * private String readFile(String filename) { try { return
	 * Files.readString(Paths.get(filename)); } catch (IOException e) {
	 * e.printStackTrace(); } return ""; }
	 */



	private String createDiagram() {
		String diagram = "";
		diagram += "digraph {\n";
		// retrieve isolated class
		List<String> isolatedClasses = new ArrayList<String>();
		for (ClassInfo classInfo : listClasses) {
			if(classInfo.getSimpleName().contains("Scope")) {
				System.out.print("createDiagram Fore debug");
			}
			if (!classInfo.hasLink()) {
				if (!classInfo.isLinkedBy(listClasses)) {
					isolatedClasses.add(classInfo.getCompleteName());
				}
			}
		}
		System.out.println("createDiagram : isolatedClasses = " + isolatedClasses);
		// nodes
		//boolean isFirst = true;
		for(String nextPackage : mapPackageColor.keySet()) {
			String clusterName = "cluster:" + nextPackage ;
			String packageColor = mapPackageColor.get(nextPackage);
			diagram += "subgraph " + addQuotes(clusterName)
					+ " {\nlabel = <<b>" + nextPackage + "</b>>"
					+ " \n" + "fontcolor = " + addQuotes(packageColor)
					+ " \n" + "fontsize = 11"
					+ " \n" + "style=bold"
					+ " \n" + "color=" + addQuotes(packageColor) + "\n";
			for (ClassInfo classInfo : listClasses) {
				if(classInfo.getPackageName().equals(nextPackage)) {
					if(!isolatedClasses.contains(classInfo.getCompleteName())) {
						String classNode = classInfo.generateGraphNode();
						diagram += classNode;
						diagram += "\n";
						//lastClassFile = classInfo;
					}
				}
			}
			diagram += "}\n";
		}

		// relations (Inheritance)
		List<String> inheritanceRelations = new ArrayList<String>();
		for (ClassInfo classInfo : listClasses) {
			String superClassName = classInfo.getSuperClassName();
			if (superClassName != null) {
				String newRelation = classInfo.getSimpleName() + " -> "
						+ ExtractorUtil.getClassSimpleName(superClassName);
				if (!inheritanceRelations.contains(newRelation) && !classInfo.getCompleteName().equals(superClassName)) {
					inheritanceRelations.add(newRelation);
				}
			}
		}
		for (String relation : inheritanceRelations) {
			diagram += relation + " [arrowhead=onormal, color=black]\n";
		}
		List<String> implementRelations = new ArrayList<String>();
		for (ClassInfo classInfo : listClasses) {
			List<String> listInterfaces = classInfo.getInterfaces();// getInterfaces(classFile);
			for (String nextInterface : listInterfaces) {
				String newRelation = classInfo.getSimpleName() + " -> "
						+ ExtractorUtil.getClassSimpleName(nextInterface);
				if (!implementRelations.contains(newRelation) && !classInfo.getCompleteName().equals(nextInterface)) {
					implementRelations.add(newRelation);
				}
			}
		}
		for (String relation : implementRelations) {
			// style=dashed
			diagram += relation + " [arrowhead=onormal, style=dashed, color=black]\n";
		}

		// relations (Association)
		List<String> propertyRelations = new ArrayList<String>();
		Map<String, String> propertyNames = new HashMap<String, String>();
		for (ClassInfo classInfo : listClasses) {
			Map<String, List<String>> listProperties = classInfo.getProperties();
			if (listProperties == null) {
				System.out.println("For debug");
			}
			for (String targetedClass : listProperties.keySet()) {
				String newRelation = classInfo.getSimpleName() + " -> "
						+ ExtractorUtil.getClassSimpleName(targetedClass);
				if (!propertyRelations.contains(newRelation) && !classInfo.getCompleteName().equals(targetedClass)) {
					propertyRelations.add(newRelation);
					List<String> propNames = listProperties.get(targetedClass);
					if(newRelation.contains("PredictionResult>") || classInfo.getSimpleName().contains("PredictionResult")) {
						System.out.print("createDiagram : for debug");
					}
					if(propNames.size() == 1)  {
						propertyNames.put(newRelation, propNames.get(0));
					} else if(propNames.size() > 1)  {
						String sPropNames = String.join("\n", propNames);
						propertyNames.put(newRelation, addQuotes(sPropNames));
					}
				}
			}
		}
		for (String relation : propertyRelations) {
			String propName = propertyNames.get(relation);
			diagram += relation + " [arrowhead=vee, color=black, label=" + propName + "]\n";
		}
		diagram += "}";
		return diagram;
	}

	private static String addQuotes(String str) {
		return ExtractorUtil.addQuotes(str);
	}

	public static void main(String[] args) {
		new ClassDiagramExtractor();
		System.exit(0);
	}

}
