package no.nav.foreldrepenger.behandling.fp;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;

public class UtledVedtakResultatType {
    private UtledVedtakResultatType() {
        // hide public contructor
    }

    public static VedtakResultatType utled(Behandling behandling, BehandlingResultatType behandlingResultatType, Optional<LocalDate> opphørsdato,
                                           Optional<LocalDate> skjæringstidspunkt) {
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(behandlingResultatType);

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING;
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            return VedtakResultatType.VEDTAK_I_ANKEBEHANDLING;
        }
        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return VedtakResultatType.VEDTAK_I_INNSYNBEHANDLING;
        }
        if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.FORELDREPENGER_ENDRET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            Behandling originalBehandling = behandling.getOriginalBehandling()
                .orElseThrow(() -> new IllegalStateException("Kan ikke ha resultat INGEN ENDRING uten å ha en original behandling"));
            return utled(originalBehandling, Optional.empty(), skjæringstidspunkt);
        }
        if (BehandlingResultatType.OPPHØR.equals(behandlingResultatType)) {
            if (opphørsdato.isPresent() && skjæringstidspunkt.isPresent() && opphørsdato.get().isAfter(skjæringstidspunkt.get())) {
                return VedtakResultatType.INNVILGET;
            }
        }
        return VedtakResultatType.AVSLAG;
    }

    public static VedtakResultatType utled(Behandling behandling, Optional<LocalDate> opphørsdato, Optional<LocalDate> skjæringstidspunkt) {
        BehandlingResultatType behandlingResultatType = behandling.getBehandlingsresultat().getBehandlingResultatType();
        return utled(behandling, behandlingResultatType, opphørsdato, skjæringstidspunkt);
    }
}
