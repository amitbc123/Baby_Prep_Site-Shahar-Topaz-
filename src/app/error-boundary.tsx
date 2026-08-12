import { Component, type ReactNode } from 'react'
import { Button } from '@/components/ui/button'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  render() {
    if (this.state.error) {
      return (
        <div className="flex min-h-dvh flex-col items-center justify-center gap-4 bg-background p-6 text-center text-foreground">
          <p className="font-heading text-2xl">משהו השתבש</p>
          <p className="max-w-sm text-sm text-muted-foreground">
            קרתה תקלה בלתי צפויה. המידע שלכם שמור במכשיר ולא נפגע — נסו לרענן את הדף.
          </p>
          <Button onClick={() => window.location.reload()}>רענון הדף</Button>
        </div>
      )
    }
    return this.props.children
  }
}
