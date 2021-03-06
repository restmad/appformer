/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.appformer.formmodeler.codegen;

import org.kie.workbench.common.forms.model.FormDefinition;
import org.kie.workbench.common.forms.model.FormModel;
import org.kie.workbench.common.forms.model.JavaFormModel;

public interface JavaSourceGenerator {

    public String generateJavaSource(SourceGenerationContext context);

    default void checkFormDefinition(final FormDefinition form) {
        final FormModel model = form.getModel();

        if (model == null) {
            throw new UnsupportedOperationException("Cannot generate RestService for an empty model.");
        }
        if (!(model instanceof JavaFormModel)) {
            throw new UnsupportedOperationException("Cannot generate RestService for a model which is not Java.");
        }
    }
}
