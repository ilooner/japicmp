package japicmp.cmp;

import japicmp.filter.Filter;
import japicmp.config.Options;
import japicmp.filter.Filters;
import japicmp.model.AccessModifier;

import java.util.LinkedList;
import java.util.List;

public class JarArchiveComparatorOptions {
	private List<String> classPathEntries = new LinkedList<>();
	private AccessModifier accessModifier = AccessModifier.PROTECTED;
    private Filters filters = new Filters();
	private boolean includeSynthetic = false;
  private boolean ignoreSynthetic = true;
	private boolean ignoreMissingClasses = false;
  private boolean ignoreBridge = true;

	public static JarArchiveComparatorOptions of(Options options) {
		JarArchiveComparatorOptions comparatorOptions = new JarArchiveComparatorOptions();
		comparatorOptions.getFilters().getExcludes().addAll(options.getExcludes());
		comparatorOptions.getFilters().getIncludes().addAll(options.getIncludes());
		comparatorOptions.setAccessModifier(options.getAccessModifier());
		comparatorOptions.setIncludeSynthetic(options.isIncludeSynthetic());
    comparatorOptions.setIgnoreSynthetic(options.isIgnoreSynthetic());
    comparatorOptions.setIgnoreBridge(options.isIgnoreBridge());
		comparatorOptions.setIgnoreMissingClasses(options.isIgnoreMissingClasses());
		return comparatorOptions;
	}

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        this.filters = filters;
    }

    public List<String> getClassPathEntries() {
		return classPathEntries;
	}

	public void setAccessModifier(AccessModifier accessModifier) {
		this.accessModifier = accessModifier;
	}

	public AccessModifier getAccessModifier() {
		return accessModifier;
	}

	public void setIncludeSynthetic(boolean includeSynthetic) {
		this.includeSynthetic = includeSynthetic;
	}

	public boolean isIncludeSynthetic() {
		return includeSynthetic;
	}

  public void setIgnoreSynthetic(boolean showNonSyntheticToSynthetic) {
    ignoreSynthetic = showNonSyntheticToSynthetic;
  }

  public boolean isIgnoreSynthetic() {
    return ignoreSynthetic;
  }

	public void setIgnoreMissingClasses(boolean ignoreMissingClasses) {
		this.ignoreMissingClasses = ignoreMissingClasses;
	}

	public boolean isIgnoreMissingClasses() {
		return ignoreMissingClasses;
	}

  public boolean isIgnoreBridge() {
    return ignoreBridge;
  }

  public void setIgnoreBridge(boolean ignoreBridge) {
    this.ignoreBridge = ignoreBridge;
  }
}
