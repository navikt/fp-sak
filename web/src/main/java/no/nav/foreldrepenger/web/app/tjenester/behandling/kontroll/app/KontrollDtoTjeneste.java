package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalGruppeWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto.KontrollresultatDto;

@ApplicationScoped
public class KontrollDtoTjeneste {

    private RisikovurderingTjeneste risikovurderingTjeneste;

    KontrollDtoTjeneste() {
        // CDI
    }

    @Inject
    public KontrollDtoTjeneste(RisikovurderingTjeneste risikovurderingTjeneste) {
        this.risikovurderingTjeneste = risikovurderingTjeneste;
    }

    public Optional<KontrollresultatDto> lagKontrollresultatForBehandling(BehandlingReferanse referanse) {
        var wrapperOpt = risikovurderingTjeneste.hentRisikoklassifisering(referanse);
        if (wrapperOpt.isEmpty()) {
            return Optional.of(KontrollresultatDto.ikkeKlassifisert());
        }
        return wrapperOpt.flatMap(this::lagDto);
    }

    private Optional<KontrollresultatDto> lagDto(FaresignalWrapper wrapper) {
        if (Kontrollresultat.HØY.equals(wrapper.kontrollresultat())) {
            var iayFaresignaler = lagFaresignalGruppeDto(wrapper.iayFaresignaler());
            var medlemskapFaresignaler = lagFaresignalGruppeDto(wrapper.medlemskapFaresignaler());
            return Optional.of(
                new KontrollresultatDto(wrapper.kontrollresultat(), iayFaresignaler, medlemskapFaresignaler, wrapper.faresignalVurdering()));
        } else {
            return Optional.of(new KontrollresultatDto(wrapper.kontrollresultat(), null, null, null));
        }
    }

    private KontrollresultatDto.FaresignalgruppeDto lagFaresignalGruppeDto(FaresignalGruppeWrapper faresignalgruppe) {
        if (faresignalgruppe == null || faresignalgruppe.faresignaler().isEmpty()) {
            return null;
        }
        return new KontrollresultatDto.FaresignalgruppeDto(faresignalgruppe.faresignaler());
    }

}
