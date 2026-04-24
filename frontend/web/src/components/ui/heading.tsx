import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "./utils"

const headingVariants = cva(
  "font-sans font-bold tracking-tight text-foreground", // Use font-sans which is mapped to Pretendard
  {
    variants: {
      level: {
        h1: "text-5xl", // 48px 고정
        h2: "text-4xl", // 36px 고정
        h3: "text-3xl", // 30px 고정
        h4: "text-2xl",
        h5: "text-xl",
        h6: "text-lg",
      },
      variant: {
        default: "text-foreground",
        brand: "text-[var(--color-brand-primary)]",
        sub: "text-[var(--color-sub-primary)]",
      }
    },
    defaultVariants: {
      level: "h2",
      variant: "default",
    },
  }
)

export interface HeadingProps
  extends React.HTMLAttributes<HTMLHeadingElement>,
    VariantProps<typeof headingVariants> {
  as?: "h1" | "h2" | "h3" | "h4" | "h5" | "h6"
}

const Heading = React.forwardRef<HTMLHeadingElement, HeadingProps>(
  ({ className, level, variant, as, ...props }, ref) => {
    const Component = as || level || "h2"
    return (
      <Component
        ref={ref}
        className={cn(headingVariants({ level, variant, className }))}
        {...props}
      />
    )
  }
)
Heading.displayName = "Heading"

export { Heading, headingVariants }
