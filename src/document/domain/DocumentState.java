package document.domain;

import java.util.List;

public record DocumentState(
    String documentId,
    List<DocumentSection> sections
) {
    public DocumentState {
        sections = List.copyOf(sections);
    }

    public DocumentRuleState ruleState() {
        return sections.isEmpty() ? DocumentRuleState.EMPTY : DocumentRuleState.HAS_SECTIONS;
    }

    public DocumentSection findSection(String sectionId) {
        if (sectionId == null) {
            return null;
        }
        return sections.stream()
            .filter(section -> section.id().equals(sectionId))
            .findFirst()
            .orElse(null);
    }

    public int indexOfSection(String sectionId) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).id().equals(sectionId)) {
                return i;
            }
        }
        return -1;
    }
}
