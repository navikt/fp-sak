package no.nav.foreldrepenger.domene.mappers.endringutleder_fra_entitet;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.oppdateringresultat.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonskravGyldighetEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class UtledFaktaOmBeregningVurderinger {

    private UtledFaktaOmBeregningVurderinger() {
        // Skjul
    }

    public static FaktaOmBeregningVurderinger utled(BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                    Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, FaktaBeregningLagreDto fakta) {
        FaktaOmBeregningVurderinger faktaOmBeregningVurderinger = new FaktaOmBeregningVurderinger();
        faktaOmBeregningVurderinger.setHarEtterlønnSluttpakkeEndring(utledVurderEtterlønnSluttpakkeEndring(fakta));
        faktaOmBeregningVurderinger.setHarLønnsendringIBeregningsperiodenEndring(utledVurderLønnsendringEndring(fakta));
        faktaOmBeregningVurderinger.setHarMilitærSiviltjenesteEndring(utledVurderMilitærEllerSiviltjenesteEndring(fakta));
        faktaOmBeregningVurderinger.setErSelvstendingNyIArbeidslivetEndring(utledErSelvstendigNyIArbeidslivetEndring(fakta));
        faktaOmBeregningVurderinger.setErNyoppstartetFLEndring(utledErNyoppstartetFLEndring(fakta));
        faktaOmBeregningVurderinger.setVurderRefusjonskravGyldighetEndringer(utledUtvidRefusjonskravGyldighetEndringer(fakta));
        faktaOmBeregningVurderinger.setErMottattYtelseEndringer(UtledErMottattYtelseEndringer.utled(grunnlag, forrigeGrunnlag));
        faktaOmBeregningVurderinger.setErTidsbegrensetArbeidsforholdEndringer(UtledErTidsbegrensetArbeidsforholdEndringer.utled(grunnlag, forrigeGrunnlag));
        if (harEndringer(faktaOmBeregningVurderinger)) {
            return faktaOmBeregningVurderinger;
        }
        return null;
    }

    private static List<RefusjonskravGyldighetEndring> utledUtvidRefusjonskravGyldighetEndringer(FaktaBeregningLagreDto fakta) {
        if (fakta.getRefusjonskravGyldighet() == null) {
            return Collections.emptyList();
        }
        return fakta.getRefusjonskravGyldighet().stream().map(UtledFaktaOmBeregningVurderinger::utledRefusjonskravGyldighetEndring)
                .collect(Collectors.toList());
    }

    private static RefusjonskravGyldighetEndring utledRefusjonskravGyldighetEndring(RefusjonskravPrArbeidsgiverVurderingDto dto) {
        return new RefusjonskravGyldighetEndring(new ToggleEndring(null, dto.isSkalUtvideGyldighet()),
                OrganisasjonsNummerValidator.erGyldig(dto.getArbeidsgiverId()) ? Arbeidsgiver.virksomhet(dto.getArbeidsgiverId()) : Arbeidsgiver.person(new AktørId(dto.getArbeidsgiverId())));
    }

    private static boolean harEndringer(FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        return faktaOmBeregningVurderinger.getErNyoppstartetFLEndring().isPresent() || faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring()
            .isPresent() || faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring().isPresent() || faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring()
            .isPresent() || faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring().isPresent() ||
                !faktaOmBeregningVurderinger.getErMottattYtelseEndringer().isEmpty() ||
                !faktaOmBeregningVurderinger.getVurderRefusjonskravGyldighetEndringer().isEmpty() ||
                !faktaOmBeregningVurderinger.getErTidsbegrensetArbeidsforholdEndringer().isEmpty();
    }

    private static ToggleEndring utledErNyoppstartetFLEndring(FaktaBeregningLagreDto fakta) {
        if (fakta.getVurderNyoppstartetFL() != null) {
            return new ToggleEndring(null, fakta.getVurderNyoppstartetFL().erErNyoppstartetFL());
        }
        return null;
    }

    private static ToggleEndring utledVurderEtterlønnSluttpakkeEndring(FaktaBeregningLagreDto fakta) {
        if (fakta.getVurderEtterlønnSluttpakke() != null) {
            return new ToggleEndring(null, fakta.getVurderEtterlønnSluttpakke().erEtterlønnSluttpakke());
        }
        return null;
    }

    private static ToggleEndring utledVurderLønnsendringEndring(FaktaBeregningLagreDto fakta) {
        if (fakta.getVurdertLonnsendring() != null) {
            return new ToggleEndring(null, fakta.getVurdertLonnsendring().erLønnsendringIBeregningsperioden());
        }
        return null;
    }

    private static ToggleEndring utledVurderMilitærEllerSiviltjenesteEndring(FaktaBeregningLagreDto fakta) {
        if (fakta.getVurderMilitaer() != null) {
            return new ToggleEndring(null, fakta.getVurderMilitaer().getHarMilitaer());
        }
        return null;
    }

    private static ToggleEndring utledErSelvstendigNyIArbeidslivetEndring(FaktaBeregningLagreDto fakta) {
        if (fakta.getVurderNyIArbeidslivet() != null) {
            return new ToggleEndring(null, fakta.getVurderNyIArbeidslivet().erNyIArbeidslivet());
        }
        return null;
    }


}
