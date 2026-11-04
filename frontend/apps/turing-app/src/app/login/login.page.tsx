import { LoginForm } from "@/components/login/login-form"
import { IconSearch, IconSparkles, IconWorld } from "@tabler/icons-react";
import { TurLogo } from "@/components/logo/tur-logo"
import { ModeToggle } from "@/components/mode-toggle"
import { Login } from "@viglet/viglet-design-system"
import { useTranslation } from "react-i18next"
import { LoginSettingsButton } from "./login-settings"
import { useLoginSettings } from "./use-login-settings"
import { guestConsoleLoginUrl, useGuestConsoleStatus } from "@/api/queries/auth.queries"

const LoginPage = () => {
  const { t } = useTranslation()
  const { settings, update } = useLoginSettings()
  const { data: guestConsole } = useGuestConsoleStatus()

  return (
    <Login color="#2563eb" colorDark="#60a5fa">
      <Login.Background
        withFormulas={settings.formulas}
        withBonds={settings.formulas}
        withLightning={settings.explosion}
        withExplosion={settings.explosion}
        withOrbs={settings.animations}
        motionPaused={!settings.animations}
        withGrid
      />

      <Login.Settings>
        <LoginSettingsButton settings={settings} onUpdate={update} />
        <ModeToggle />
      </Login.Settings>

      <Login.Content>
        <Login.Logo pulsePaused={!settings.animations}>
          <TurLogo size={52} />
        </Login.Logo>

        <Login.Title>Viglet Turing</Login.Title>
        <Login.Tagline>{t("login.heroTagline")}</Login.Tagline>

        <Login.Features>
          <Login.FeaturePill icon={<IconSearch className="h-3.5 w-3.5 text-blue-500" />}>
            {t("login.featureSearch")}
          </Login.FeaturePill>
          <Login.FeaturePill icon={<IconSparkles className="h-3.5 w-3.5 text-indigo-500" />}>
            {t("login.featureAi")}
          </Login.FeaturePill>
          <Login.FeaturePill icon={<IconWorld className="h-3.5 w-3.5 text-cyan-500" />}>
            {t("login.featureKnowledge")}
          </Login.FeaturePill>
        </Login.Features>

        <Login.Card>
          <LoginForm />
        </Login.Card>

        {/* T1184 — a demo host's read-only guest console, offered only when the server says so. */}
        {guestConsole?.enabled && (
          <a
            href={guestConsoleLoginUrl()}
            className="mt-4 block text-center text-sm font-medium text-blue-600 hover:underline dark:text-blue-400"
          >
            {t("login.exploreAsGuest")}
            <span className="block text-xs font-normal text-muted-foreground">
              {t("login.exploreAsGuestHint")}
            </span>
          </a>
        )}

        <Login.Footer>
          Viglet Turing &mdash; Enterprise{" "}
          <IconSearch className="inline h-3.5 w-3.5 align-[-2px] text-blue-500" />{" "}
          Intelligence
        </Login.Footer>
      </Login.Content>
    </Login>
  )
}

export default LoginPage
