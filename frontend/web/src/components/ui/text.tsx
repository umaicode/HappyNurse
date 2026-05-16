import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "./utils"

const textVariants = cva(
  "text-foreground",
  {
    variants: {
      variant: {
        default: "text-foreground",
        muted: "text-muted-foreground",
        brand: "text-[var(--color-brand-primary)]",
        sub: "text-[var(--color-sub-primary)]",
        action: "text-[var(--color-action-blue)]",
        white: "text-white",
        error: "text-destructive",
      },
      size: {
        default: "text-sm",
        xs: "text-xs",
        sm: "text-sm",
        base: "text-base",
        lg: "text-lg",
        xl: "text-xl",
      },
      weight: {
        default: "font-normal",
        medium: "font-medium",
        semibold: "font-semibold",
        bold: "font-bold",
        extrabold: "font-extrabold",
      },
      mono: {
        true: "tabular-nums tracking-tight",
        false: "",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
      weight: "default",
      mono: false,
    },
  }
)

export interface TextProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof textVariants> {
  as?: React.ElementType
}

const Text = React.forwardRef<HTMLSpanElement, TextProps>(
  ({ className, variant, size, weight, mono, as: Component = "span", ...props }, ref) => {
    return (
      <Component
        ref={ref}
        className={cn(textVariants({ variant, size, weight, mono, className }))}
        {...props}
      />
    )
  }
)
Text.displayName = "Text"

export { Text, textVariants }
