package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
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

    public static FaktaOmBeregningVurderinger utled(Optional<FaktaAggregat> fakta,
                                                    Optional<FaktaAggregat> forrigeFakta,
                                                    FaktaBeregningLagreDto faktaLagreDto) {
        FaktaOmBeregningVurderinger faktaOmBeregningVurderinger = new FaktaOmBeregningVurderinger();
        faktaOmBeregningVurderinger.setHarEtterlønnSluttpakkeEndring(utledVurderEtterlønnSluttpakkeEndring(faktaLagreDto));
        faktaOmBeregningVurderinger.setHarLønnsendringIBeregningsperiodenEndring(utledVurderLønnsendringEndring(faktaLagreDto));
        faktaOmBeregningVurderinger.setHarMilitærSiviltjenesteEndring(utledVurderMilitærEllerSiviltjenesteEndring(faktaLagreDto));
        faktaOmBeregningVurderinger.setErSelvstendingNyIArbeidslivetEndring(utledErSelvstendigNyIArbeidslivetEndring(faktaLagreDto));
        faktaOmBeregningVurderinger.setErNyoppstartetFLEndring(utledErNyoppstartetFLEndring(faktaLagreDto));
        faktaOmBeregningVurderinger.setVurderRefusjonskravGyldighetEndringer(utledUtvidRefusjonskravGyldighetEndringer(faktaLagreDto));
        fakta.ifPresent(fa -> faktaOmBeregningVurderinger.setErMottattYtelseEndringer(UtledErMottattYtelseEndringer.utled(fa, forrigeFakta)));
        fakta.ifPresent(fa -> faktaOmBeregningVurderinger.setErTidsbegrensetArbeidsforholdEndringer(UtledErTidsbegrensetArbeidsforholdEndringer.utled(fa, forrigeFakta)));
        if (harEndringer(faktaOmBeregningVurderinger)) {
            return faktaOmBeregningVurderinger;
        }
        return null;
    }

    private static List<RefusjonskravGyldighetEndring> utledUtvidRefusjonskravGyldighetEndringer(FaktaBeregningLagreDto fakta) {
        if (fakta.getRefusjonskravGyldighet() == null) {
            return Collections.emptyList();
        }
        return fakta.getRefusjonskravGyldighet().stream()
            .map(UtledFaktaOmBeregningVurderinger::utledRefusjonskravGyldighetEndring)
                .collect(Collectors.toList());
    }

    private static RefusjonskravGyldighetEndring utledRefusjonskravGyldighetEndring(RefusjonskravPrArbeidsgiverVurderingDto dto) {
        return new RefusjonskravGyldighetEndring(new ToggleEndring(null, dto.isSkalUtvideGyldighet()),
                OrganisasjonsNummerValidator.erGyldig(dto.getArbeidsgiverId()) ? Arbeidsgiver.virksomhet(dto.getArbeidsgiverId()) : Arbeidsgiver.person(new AktørId(dto.getArbeidsgiverId())));
    }

    private static boolean harEndringer(FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        return faktaOmBeregningVurderinger.getErNyoppstartetFLEndring() != null ||
                faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring() != null ||
                faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring() != null ||
                faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring() != null ||
                faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring() != null ||
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
