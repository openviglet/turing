# Turing Shadcn

This is the React + TypeScript + shadcn/ui version of the Turing UI project, converted from the original Angular implementation.

## Project Structure

- `/src/app/console` - Main console application (admin interface)
- `/src/app/welcome` - Login and authentication
- `/src/app/sn` - Search templates (Semantic Navigation)
- `/src/components/ui` - shadcn/ui components
- `/src/lib` - Utility functions
- `/src/models` - TypeScript models/interfaces
- `/src/services` - API services

## Original Angular Projects Converted

This project consolidates the following Angular applications from `turing-ui`:

1. **Console** - Main admin interface with modules:
   - Semantic Navigation (SN)
   - Search Engine (SE)
   - Integration
   - LLM (Large Language Models)
   - Logging
   - Store
   - Token Management

2. **Welcome** - Login and authentication interface

3. **SN** - Search templates and semantic navigation

## Development

```bash
npm install
npm run dev
```

## Build

```bash
npm run compile
```

The build output will be placed in `../turing-app/src/main/resources/public/shadcn`

## Technology Stack

- **React 19** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool
- **Tailwind CSS v4** - Styling
- **shadcn/ui** - Component library (built on Radix UI)
- **React Router** - Navigation
- **Axios** - HTTP client
- **Sonner** - Toast notifications
- **Lucide React** - Icons
