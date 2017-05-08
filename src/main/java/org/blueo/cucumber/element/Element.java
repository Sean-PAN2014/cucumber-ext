package org.blueo.cucumber.element;

import static org.blueo.cucumber.util.ElementUtils.TYPE_CONVERTER;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.blueo.cucumber.keyword.YesOrNo;
import org.blueo.cucumber.util.ElementUtils;
import org.blueo.cucumber.util.FieldLocator;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.Assert;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Element {
	public static final String IGNORE_ROW = "_ignoreRow";
	public static final String EXPECT_FAIL = "_expectFail";
	// 
	protected final Map<String, String> keyValueMap;

	public Element(Map<String, String> keyValueMap) {
		this.keyValueMap = Maps.newHashMap(keyValueMap);
	}

	public <T> Optional<T> getValueOpt(String key, Class<T> type) {
		return Optional.ofNullable(this.getValue(key, type));
	}

	public <T> T getValue(String key, Class<T> type) {
		String value = keyValueMap.get(key);
		Assert.notNull(value, String.format("Not value found for key[%s]", key));
		return TYPE_CONVERTER.convertIfNecessary(value, type);
	}

	public <T> T getValue(String key, T defaultValue) {
		Preconditions.checkNotNull(defaultValue);
		String value = keyValueMap.get(key);
		if (value == null) {
			return defaultValue;
		}
		// 
		@SuppressWarnings("unchecked")
		T t = this.getValue(key, (Class<T>) defaultValue.getClass());
		if (t == null) {
			return defaultValue;
		} else {
			return t;
		}
	}

	public void putValuesTo(Object target) {
		BeanWrapperImpl beanWrapperImpl = new BeanWrapperImpl(target);
		for (Entry<String, String> e : this.getEligibleEntries()) {
			FieldLocator fieldLocator = new FieldLocator(e.getKey());
			initNullObjectIfNeeded(beanWrapperImpl, fieldLocator.getPrefixes());
			beanWrapperImpl.setPropertyValue(e.getKey(), e.getValue());
		}
	}

	private void initNullObjectIfNeeded(BeanWrapperImpl beanWrapperImpl, String[] prefixes) {
		String propertyName = "";
		for (int i = 0; i < prefixes.length; i++) {
			propertyName += prefixes[i];
			Object propertyValue = beanWrapperImpl.getPropertyValue(propertyName);
			if (propertyValue == null) {
				Class<?> propertyType = beanWrapperImpl.getPropertyType(propertyName);
				Object instantiate = ElementUtils.newInstance(propertyType);
				beanWrapperImpl.setPropertyValue(propertyName, instantiate);
			}
			propertyName += ".";
		}
	}

	// -----------------------------
	// ----- built-in
	// -----------------------------

	public boolean isIgnoreRow() {
		YesOrNo yesOrNo = this.getValue(IGNORE_ROW, YesOrNo.NO);
		return yesOrNo.isYes();
	}

	public boolean isNotIgnoreRow() {
		return !this.isIgnoreRow();
	}

	public Map<String, String> dataMap() {
		return keyValueMap;
	}

	/**
	 * To Evaluate if self values matches Target Obejct's fields' values according to keys
	 * 
	 * Field Match Logic
	 * 
	 * For Comparable -> using compare
	 * For None Comparable -> using equals
	
	 * @param target
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean matches(Object target) {
		// 
		BeanWrapperImpl beanWrapperImpl = new BeanWrapperImpl(target);
		for (Entry<String, String> e : this.getEligibleEntries()) {
			Object actualValue = beanWrapperImpl.getPropertyValue(e.getKey());
			Object expectedValue = ElementUtils.TYPE_CONVERTER.convertIfNecessary(e.getValue(), beanWrapperImpl.getPropertyType(e.getKey()));
			// 
			if (actualValue instanceof Comparable<?> && expectedValue instanceof Comparable<?>) {
				if (((Comparable<Object>) actualValue).compareTo(expectedValue) != 0) {
					return false;
				}
			} else {
				if (!Objects.equals(actualValue, expectedValue)) {
					return false;
				}
			}
		}
		return true;
	}

	public void convert(String oldKey, String newKey, Function<String, ?> valueConverter) {
		if (!keyValueMap.containsKey(oldKey)) {
			return;
		}
		String oldValue = keyValueMap.remove(oldKey);
		Object newValue = valueConverter.apply(oldValue);
		keyValueMap.put(newKey, String.valueOf(newValue));
	}

	public Set<String> getKeySet() {
		return Sets.newHashSet(keyValueMap.keySet());
	}

	public boolean isExpectFail() {
		YesOrNo yesOrNo = this.getValue(EXPECT_FAIL, YesOrNo.NO);
		return yesOrNo.isYes();
	}

	/**
	 * Get entries, ignoring followings
	 * <li> key start with "_"
	 * <li> value is empty
	 * <br>
	 * 
	 * @return
	 */
	private List<Map.Entry<String, String>> getEligibleEntries() {
		// @formatter:off
		return keyValueMap.entrySet().stream()
			.filter(e -> !e.getKey().startsWith("_"))
			.filter(e -> StringUtils.isNotEmpty(e.getValue()))
			.collect(Collectors.toList());
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName());
		builder.append(" [keyValueMap=");
		builder.append(keyValueMap);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((keyValueMap == null) ? 0 : keyValueMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Element other = (Element) obj;
		if (keyValueMap == null) {
			if (other.keyValueMap != null)
				return false;
		} else if (!keyValueMap.equals(other.keyValueMap))
			return false;
		return true;
	}

}
