package gov.healthit.chpl.web.controller.results;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Results object when verifying Certification IDs "en masse".
 */
public class CertificationIdVerifyResponse implements Serializable {
    private static final long serialVersionUID = -3582436342627660622L;
    private List<VerifyResult> results = new ArrayList<VerifyResult>();

    public CertificationIdVerifyResponse() {
    }

    public CertificationIdVerifyResponse(Map<String, Boolean> map) {
        this.importMap(map);
    }

    public List<VerifyResult> getResults() {
        return this.results;
    }

    private void importMap(Map<String, Boolean> map) {
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            VerifyResult item = new VerifyResult(entry.getKey(), entry.getValue());
            this.results.add(item);
        }
    }

    /**
     * Single result inside Cert ID verification result.
     */
    @Data
    public static class VerifyResult implements Serializable {
        private static final long serialVersionUID = -85566386396366634L;
        private String id;
        private boolean valid;

        public VerifyResult(String id, Boolean valid) {
            this.id = id;
            this.valid = valid;
        }
    }
}
