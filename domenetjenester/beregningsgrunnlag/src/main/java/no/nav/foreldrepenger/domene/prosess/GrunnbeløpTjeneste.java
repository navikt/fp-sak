package no.nav.foreldrepenger.domene.prosess;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;

@ApplicationScoped
public class GrunnbeløpTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    public GrunnbeløpTjeneste() {
        // For CDI
    }

    @Inject
    public GrunnbeløpTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public List<Grunnbeløp> mapGrunnbeløpSatser() {
        List<Grunnbeløp> grunnbeløpListe = new ArrayList<>();
        var iår = LocalDate.now().getYear();
        for (var år = 2000; år <= iår; år++) {
            // Den vil ikke plukke opp alle grunnbeløp hvis det blir endret f.eks to ganger i året .
            var dato = LocalDate.now().withYear(år);
            var grunnbeløp = grunnbeløpOgSnittFor(dato);
            grunnbeløpListe.add(grunnbeløp);
        }
        return grunnbeløpListe;
    }

    private Grunnbeløp grunnbeløpOgSnittFor(LocalDate dato) {
        var g = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, dato);
        var gSnitt = beregningsgrunnlagRepository.finnEksaktSats(BeregningSatsType.GSNITT, g.getPeriode().getFomDato());
        return new Grunnbeløp(g.getPeriode().getFomDato(), g.getPeriode().getTomDato(), g.getVerdi(), gSnitt.getVerdi());
    }

}
