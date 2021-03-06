/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.appformer.formmodeler.codegen.view.impl.inputs.impl;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.appformer.formmodeler.codegen.view.impl.java.inputs.impl.IntegerBoxHelper;
import org.kie.workbench.common.forms.fields.shared.fieldTypes.basic.integerBox.definition.IntegerBoxFieldDefinition;

import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IntegerBoxHelperTest {

    private static final String INPUT_WIDGET_CLASS_NAME = "org.kie.workbench.common.forms.common.rendering.client.widgets.integerBox.IntegerBox";
    IntegerBoxHelper helper;

    @Mock
    IntegerBoxFieldDefinition mockFieldDefinition;

    @Before
    public void initHelper() {
        helper = new IntegerBoxHelper();
    }

    @Test
    public void testGetSupportedFieldTypeCode() {
        String expectedTypeCode = IntegerBoxFieldDefinition.FIELD_TYPE.getTypeName();
        String actualTypeCode = helper.getSupportedFieldTypeCode();
        assertEquals(actualTypeCode,
                     expectedTypeCode);
    }

    @Test
    public void testGetInputWidget() {
        String expectedInputWidget = INPUT_WIDGET_CLASS_NAME;
        String actualInputWidget = helper.getInputWidget(null);
        assertEquals(actualInputWidget,
                     expectedInputWidget);
    }

    @Test
    public void testGetReadOnlyMethod() {

        String fieldName = "testFieldName";
        String readOnlyParam = "readOnly";
        String actualString = helper.getReadonlyMethod(fieldName,
                                                       readOnlyParam);

        String expectedString = fieldName + ".setEnabled( !" + readOnlyParam + ");";
        ;

        assertEquals(actualString,
                     expectedString);
    }

    @Test
    public void testGetConverterClassNameByte() {
        testGetConverterClassName(Byte.class);
    }

    @Test
    public void testGetConverterClassNameShort() {
        testGetConverterClassName(Short.class);
    }

    @Test
    public void testGetConverterClassNameInteger() {
        testGetConverterClassName(Integer.class);
    }

    @Test
    public void testGetConverterClassNameBigInteger() {
        testGetConverterClassName(BigInteger.class);
    }

    protected void testGetConverterClassName(Class fieldClass) {

        Mockito.when(mockFieldDefinition.getStandaloneClassName()).thenReturn(fieldClass.getCanonicalName());
        String actualConverter = helper.getConverterClassName(mockFieldDefinition);
        String prefix = "org.kie.workbench.common.forms.common.rendering.client.widgets.integerBox.converters.";
        String expectedConverter = prefix + fieldClass.getSimpleName() + "ToLongConverter";

        assertEquals(actualConverter,
                     expectedConverter);
    }
}
