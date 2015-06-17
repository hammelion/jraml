package com.github.hammelion.parsers;

import com.github.hammelion.exceptions.RamlFileNotFoundException;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.parser.visitor.RamlDocumentBuilder;

import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO JavaDoc in org.jraml.parsers
 */
@Named
public class RAMLParserFacade {
    private Map<String, Raml> ramlFiles = new HashMap<String, Raml>();
    private final RamlDocumentBuilder ramlBuilder = new RamlDocumentBuilder();

    public Resource findResource(String ramlFilePath, String resourceKey) throws RamlFileNotFoundException {
        // TODO Validation List<ValidationResult> results = RamlValidationService.createDefault().validate(ramlLocation);
        Raml raml = this.ramlFiles.get(ramlFilePath);
        if (raml == null) {
            try {
                raml = this.ramlBuilder.build(ramlFilePath);
            } catch (NullPointerException e) {
                throw new RamlFileNotFoundException(ramlFilePath);
            }
            this.ramlFiles.put(ramlFilePath, raml);
        }
        return raml.getResource(resourceKey);
    }
}
