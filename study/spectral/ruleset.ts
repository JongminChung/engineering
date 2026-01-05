import type {
    RulesetDefinition,
    RulesetFunction,
} from "@stoplight/spectral-core";
import spectralRulesets from "@stoplight/spectral-rulesets";

type InfoObject = Record<string, unknown> | null | undefined;

const requireExtension: RulesetFunction<InfoObject, { field: string }> = (
    input,
    options,
    context,
) => {
    if (!input || typeof input !== "object") {
        return;
    }

    if (Object.hasOwn(input, options.field)) {
        return;
    }

    return [
        {
            message: `Info object must have "${options.field}" extension.`,
            path: [...context.path, options.field],
        },
    ];
};

const { oas } =
    spectralRulesets as typeof import("@stoplight/spectral-rulesets");
const baseRuleset = oas as RulesetDefinition;

export const ruleset: RulesetDefinition = {
    extends: [baseRuleset],
    rules: {
        "info-x-service": {
            description: "Info object must define x-service extension.",
            severity: "warn",
            given: "$.info",
            // biome-ignore lint/suspicious/noThenProperty: <is not promise them>
            then: {
                function: requireExtension,
                functionOptions: {
                    field: "x-service",
                },
            },
        },
    },
};
