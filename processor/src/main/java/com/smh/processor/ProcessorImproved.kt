package com.smh.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.smh.annotation.NXDateTimeExtension
import com.smh.annotation.NXDateToString
import com.smh.annotation.NXLongToDate
import com.smh.annotation.NXLongToString
import com.smh.annotation.NXStringToDate
import com.smh.annotation.NXStringToString
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.Date
import kotlin.reflect.KClass

class NXDateTimeProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedClasses = resolver
            .getSymbolsWithAnnotation(NXDateTimeExtension::class.qualifiedName.toString())
            .filterIsInstance<KSClassDeclaration>()
            .also { logger.warn("Generating for ${it.joinToString { it.simpleName.getShortName() }}") }

        annotatedClasses.forEach { annotatedClass ->
            generateExtensionFile(annotatedClass)
        }

        return emptyList()
    }

    private fun generateExtensionFile(annotatedClass: KSClassDeclaration) {
        FileSpec.builder(
            annotatedClass.packageName.asString(),
            fileName = annotatedClass.simpleName.asString() + "_NXDateExtension"
        )
            .apply {
                annotatedClass.getDeclaredProperties()
                    .forEach { property ->
                        buildExtensionProperties(annotatedClass, property)
                    }
            }
            .build()
            .writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(aggregating = false, annotatedClass.containingFile!!)
            )
    }

    @OptIn(KspExperimental::class)
    private fun FileSpec.Builder.buildExtensionProperties(
        annotatedClass: KSClassDeclaration,
        property: KSPropertyDeclaration
    ) {
        val stringToStringAnnotations = property.getAnnotationsByType(NXStringToString::class)
        val longToStringAnnotations = property.getAnnotationsByType(NXLongToString::class)
        val dateToStringAnnotations = property.getAnnotationsByType(NXDateToString::class)
        val stringToDateAnnotations = property.getAnnotationsByType(NXStringToDate::class)
        val longToDateAnnotations = property.getAnnotationsByType(NXLongToDate::class)

        val propertyName = property.simpleName.asString()
        val propertyType = property.type.resolve()
        val propertyTypeName = propertyType.declaration.qualifiedName?.asString()

        when (propertyTypeName) {
            "kotlin.String" -> {
                handleStringTypeProperty(
                    annotatedClass,
                    property,
                    stringToStringAnnotations,
                    stringToDateAnnotations
                )
            }
            "kotlin.Long" -> {
                handleLongTypeProperty(
                    annotatedClass,
                    property,
                    longToStringAnnotations,
                    longToDateAnnotations
                )
            }
            "java.util.Date" -> {
                handleDateTypeProperty(annotatedClass, property, dateToStringAnnotations)
            }
            else -> {
                logger.error("Unsupported type: property $propertyTypeName\nCheck ${annotatedClass.simpleName.asString()}.$propertyName")
            }
        }
    }

    private fun FileSpec.Builder.handleStringTypeProperty(
        annotatedClass: KSClassDeclaration,
        property: KSPropertyDeclaration,
        stringToStringAnnotations: Sequence<NXStringToString>,
        stringToDateAnnotations: Sequence<NXStringToDate>
    ) {
        val propertyName = property.simpleName.asString()
        if ((stringToStringAnnotations.any() || stringToDateAnnotations.any())) {
            val isUsingMultipleAnnotations = stringToStringAnnotations.any() && stringToDateAnnotations.any()
            buildProperty(
                annotations = stringToStringAnnotations,
                annotatedClass = annotatedClass,
                generatePropertyType = String::class,
                generatePropertyName = { annotation ->
                    if (isUsingMultipleAnnotations) "${annotation.prefixName}String_$propertyName"
                    else "${annotation.prefixName}$propertyName"
                },
                generatePropertyReturn = { annotation ->
                    "return this.$propertyName.changeFormatDate(originPattern = \"${annotation.originPattern}\", targetPattern = \"${annotation.targetPattern}\")"
                }
            )

            buildNullableProperty(
                annotations = stringToDateAnnotations,
                annotatedClass = annotatedClass,
                generatePropertyType = Date::class,
                generatePropertyName = { annotation ->
                    if (isUsingMultipleAnnotations) "${annotation.prefixName}Date_$propertyName"
                    else "${annotation.prefixName}$propertyName"
                },
                generatePropertyReturn = { annotation ->
                    "return this.$propertyName.toDate(originPattern = \"${annotation.originPattern}\")"}
            )
        } else {
            logger.error("Annotations for String property are missing or incorrect.\nCheck ${annotatedClass.simpleName.asString()}.$propertyName")
        }
    }

    private fun FileSpec.Builder.handleLongTypeProperty(
        annotatedClass: KSClassDeclaration,
        property: KSPropertyDeclaration,
        longToStringAnnotations: Sequence<NXLongToString>,
        longToDateAnnotations: Sequence<NXLongToDate>
    ) {
        val propertyName = property.simpleName.asString()
        if (longToStringAnnotations.any() || longToDateAnnotations.any()) {
            val isUsingMultipleAnnotations = longToStringAnnotations.any() && longToDateAnnotations.any()
            buildProperty(
                annotations = longToStringAnnotations,
                annotatedClass = annotatedClass,
                generatePropertyType = String::class,
                generatePropertyName = { annotation ->
                    if (isUsingMultipleAnnotations) "${annotation.prefixName}String_$propertyName"
                    else "${annotation.prefixName}$propertyName"
                },
                generatePropertyReturn = { annotation ->
                    "return this.$propertyName.changeFormatDate(targetPattern = \"${annotation.targetPattern}\")"
                }
            )

            buildNullableProperty(
                annotations = longToDateAnnotations,
                annotatedClass = annotatedClass,
                generatePropertyType = Date::class,
                generatePropertyName = { annotation ->
                    if (isUsingMultipleAnnotations) "${annotation.prefixName}Date_$propertyName"
                    else "${annotation.prefixName}$propertyName"
                },
                generatePropertyReturn = {
                    "return this.$propertyName.toDate()"
                }
            )
        } else {
            logger.error("Annotations for Long property are missing or incorrect.\nCheck ${annotatedClass.simpleName.asString()}.$propertyName")
        }
    }

    private fun FileSpec.Builder.handleDateTypeProperty(
        annotatedClass: KSClassDeclaration,
        property: KSPropertyDeclaration,
        dateToStringAnnotations: Sequence<NXDateToString>
    ) {
        val propertyName = property.simpleName.asString()
        if (dateToStringAnnotations.any()) {
            buildProperty(
                annotations = dateToStringAnnotations,
                annotatedClass = annotatedClass,
                generatePropertyType = String::class,generatePropertyName = { annotation ->
                    "${annotation.prefixName}$propertyName"
                },
                generatePropertyReturn = { annotation ->
                    "return this.$propertyName.changeFormatDate(targetPattern = \"${annotation.targetPattern}\")"
                }
            )
        } else {
            logger.error("NXDate2String annotation is missing for Date property.\nCheck ${annotatedClass.simpleName.asString()}.$propertyName")
        }
    }

    private fun <T : Annotation> FileSpec.Builder.buildProperty(
        annotations: Sequence<T>,
        annotatedClass: KSClassDeclaration,
        generatePropertyType: KClass<*>,
        generatePropertyName: (T) -> String,
        generatePropertyReturn: (T) -> String
    ) {
        annotations.forEach { annotation ->
            addImport("com.smh.annotation", "changeFormatDate", "toDate") // Combined imports

            val packageName = annotatedClass.packageName.asString()
            val className = annotatedClass.qualifiedName?.asString().toString()

            addProperty(
                PropertySpec.builder(generatePropertyName(annotation), generatePropertyType)
                    .receiver(ClassName(packageName, className))
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(generatePropertyReturn(annotation))
                            .build()
                    )
                    .build()
            )
        }
    }

    private fun <T : Annotation> FileSpec.Builder.buildNullableProperty(
        annotations: Sequence<T>,
        annotatedClass: KSClassDeclaration,
        generatePropertyType: KClass<*>,
        generatePropertyName: (T) -> String,
        generatePropertyReturn: (T) -> String
    ) {
        annotations.forEach { annotation ->
            addImport("com.smh.annotation", "changeFormatDate", "toDate") // Combined imports

            val packageName = annotatedClass.packageName.asString()
            val className = annotatedClass.qualifiedName?.asString().toString()

            addProperty(
                PropertySpec.builder(generatePropertyName(annotation), generatePropertyType.asTypeName().copy(nullable = true))
                    .receiver(ClassName(packageName, className))
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(generatePropertyReturn(annotation))
                            .build()
                    )
                    .build()
            )
        }
    }
}