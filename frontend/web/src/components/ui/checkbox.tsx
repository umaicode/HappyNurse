"use client";

import * as React from "react";
import { Checkbox as CheckboxPrimitive } from "radix-ui";
import { CheckIcon, MinusIcon } from "lucide-react";

import { cn } from "./utils";

function Checkbox({
  className,
  ...props
}: React.ComponentProps<typeof CheckboxPrimitive.Root>) {
  return (
    <CheckboxPrimitive.Root
      data-slot="checkbox"
      className={cn(
        "group/checkbox peer size-4 shrink-0 rounded-[3px] border-2 border-[var(--color-border-hover)] bg-[var(--color-surface-card)] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-brand-primary)]/30 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:border-[var(--color-brand-primary)] data-[state=checked]:bg-[var(--color-brand-primary)] data-[state=checked]:text-white data-[state=indeterminate]:border-[var(--color-brand-primary)] data-[state=indeterminate]:bg-[var(--color-brand-primary)] data-[state=indeterminate]:text-white",
        className,
      )}
      {...props}
    >
      <CheckboxPrimitive.Indicator
        data-slot="checkbox-indicator"
        className="flex items-center justify-center text-current"
      >
        <CheckIcon
          className="size-3 group-data-[state=indeterminate]/checkbox:hidden"
          strokeWidth={3}
        />
        <MinusIcon
          className="size-3 hidden group-data-[state=indeterminate]/checkbox:block"
          strokeWidth={3}
        />
      </CheckboxPrimitive.Indicator>
    </CheckboxPrimitive.Root>
  );
}

export { Checkbox };
