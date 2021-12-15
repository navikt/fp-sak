package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalGruppeWrapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto.FaresignalgruppeDto;
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

    public Optional<KontrollresultatDto> lagKontrollresultatForBehandling(Behandling behandling) {
        var risikoklassifiseringEntitet = risikovurderingTjeneste.hentRisikoklassifiseringForBehandling(behandling.getId());
        if (risikoklassifiseringEntitet.isEmpty()) {
            return Optional.of(KontrollresultatDto.ikkeKlassifisert());
        }
        var entitet = risikoklassifiseringEntitet.get();
        if (entitet.erHøyrisiko()) {
            var faresignalWrapper = risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling);
            var iayFaresignaler = faresignalWrapper.map(wr -> lagFaresignalDto(wr.iayFaresignaler())).orElse(null);
            var medlemskapFaresignaler = faresignalWrapper.map(wr -> lagFaresignalDto(wr.medlemskapFaresignaler())).orElse(null);
            return Optional.of(new KontrollresultatDto(entitet.getKontrollresultat(), iayFaresignaler, medlemskapFaresignaler, entitet.getFaresignalVurdering()));
        } else {
            return Optional.of(new KontrollresultatDto(entitet.getKontrollresultat(), null, null, null));
        }
    }

    private FaresignalgruppeDto lagFaresignalDto(FaresignalGruppeWrapper faresignalgruppe) {
        if (faresignalgruppe == null || faresignalgruppe.faresignaler().isEmpty()) {
            return null;
        }

        return new FaresignalgruppeDto(faresignalgruppe.faresignaler());
    }

}
