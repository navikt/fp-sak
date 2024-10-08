package no.nav.foreldrepenger.domene.prosess;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.GrunnbeløpInput;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;

@ApplicationScoped
public class GrunnbeløpTjeneste {

    private SatsRepository satsRepository;

    public GrunnbeløpTjeneste() {
        // For CDI
    }

    @Inject
    public GrunnbeløpTjeneste(SatsRepository satsRepository) {
        this.satsRepository = satsRepository;
    }

    public List<GrunnbeløpInput> mapGrunnbeløpSatser() {
        List<GrunnbeløpInput> grunnbeløpListe = new ArrayList<>();
        var iår = LocalDate.now().getYear();
        for (var år = 2000; år <= iår; år++) {
            // Den vil ikke plukke opp alle grunnbeløp hvis det blir endret f.eks to ganger i året .
            var dato = LocalDate.now().withYear(år);
            var grunnbeløp = grunnbeløpOgSnittFor(dato);
            grunnbeløpListe.add(grunnbeløp);
        }
        return grunnbeløpListe;
    }

    private GrunnbeløpInput grunnbeløpOgSnittFor(LocalDate dato) {
        var g = satsRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, dato);
        var gSnitt = satsRepository.finnEksaktSats(BeregningSatsType.GSNITT, g.getPeriode().getFomDato());
        return new GrunnbeløpInput(g.getPeriode().getFomDato(), g.getPeriode().getTomDato(), g.getVerdi(), gSnitt.getVerdi());
    }

}
