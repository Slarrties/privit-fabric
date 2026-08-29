package dev.slarrties.privit.server.region.protection;

import dev.slarrties.privit.common.region.rule.Rule;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AssociatedRule {
    Rule[] value();
}