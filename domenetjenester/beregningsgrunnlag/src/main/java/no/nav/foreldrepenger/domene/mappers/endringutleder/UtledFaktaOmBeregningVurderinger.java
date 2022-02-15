package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


import no.nav.folketrygdloven.kalkulus.håndtering.v1.fakta.FaktaOmBeregningHåndteringDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrganisasjonsNummerValidator;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.oppdateringresultat.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonskravGyldighetEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class UtledFaktaOmBeregningVurderinger {

    private UtledFaktaOmBeregningVurderinger() {
        // Skjul
    }

    public static FaktaOmBeregningVurderinger utled(VurderFaktaOmBeregningDto dto,
                                                    BeregningsgrunnlagGrunnlagEntitet grunnlag,
                                                    Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag) {
        FaktaOmBeregningVurderinger faktaOmBeregningVurderinger = new FaktaOmBeregningVurderinger();
        faktaOmBeregningVurderinger.setHarEtterlønnSluttpakkeEndring(utledVurderEtterlønnSluttpakkeEndring(dto));
        faktaOmBeregningVurderinger.setHarLønnsendringIBeregningsperiodenEndring(utledVurderLønnsendringEndring(dto));
        faktaOmBeregningVurderinger.setHarMilitærSiviltjenesteEndring(utledVurderMilitærEllerSiviltjenesteEndring(dto));
        faktaOmBeregningVurderinger.setErSelvstendingNyIArbeidslivetEndring(utledErSelvstendigNyIArbeidslivetEndring(dto));
        faktaOmBeregningVurderinger.setErNyoppstartetFLEndring(utledErNyoppstartetFLEndring(dto));
        faktaOmBeregningVurderinger.setVurderRefusjonskravGyldighetEndringer(utledUtvidRefusjonskravGyldighetEndringer(dto));
        faktaOmBeregningVurderinger.setErMottattYtelseEndringer(UtledErMottattYtelseEndringer.utled(grunnlag, forrigeGrunnlag));
        faktaOmBeregningVurderinger.setErTidsbegrensetArbeidsforholdEndringer(UtledErTidsbegrensetArbeidsforholdEndringer.utled(grunnlag, forrigeGrunnlag));
        if (harEndringer(faktaOmBeregningVurderinger)) {
            return faktaOmBeregningVurderinger;
        }
        return null;
    }

    private static List<RefusjonskravGyldighetEndring> utledUtvidRefusjonskravGyldighetEndringer(VurderFaktaOmBeregningDto dto) {
        if (dto.getFakta().getRefusjonskravGyldighet() == null) {
            return Collections.emptyList();
        }
        return dto.getFakta().getRefusjonskravGyldighet().stream().map(UtledFaktaOmBeregningVurderinger::utledRefusjonskravGyldighetEndring)
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

    private static ToggleEndring utledErNyoppstartetFLEndring(VurderFaktaOmBeregningDto dto) {
        if (dto.getFakta().getVurderNyoppstartetFL() != null) {
            return new ToggleEndring(null, dto.getFakta().getVurderNyoppstartetFL().erErNyoppstartetFL());
        }
        return null;
    }

    private static ToggleEndring utledVurderEtterlønnSluttpakkeEndring(VurderFaktaOmBeregningDto dto) {
        if (dto.getFakta().getVurderEtterlønnSluttpakke() != null) {
            return new ToggleEndring(null, dto.getFakta().getVurderEtterlønnSluttpakke().erEtterlønnSluttpakke());
        }
        return null;
    }

    private static ToggleEndring utledVurderLønnsendringEndring(VurderFaktaOmBeregningDto dto) {
        if (dto.getFakta().getVurdertLonnsendring() != null) {
            return new ToggleEndring(null, dto.getFakta().getVurdertLonnsendring().erLønnsendringIBeregningsperioden());
        }
        return null;
    }

    private static ToggleEndring utledVurderMilitærEllerSiviltjenesteEndring(VurderFaktaOmBeregningDto dto) {
        if (dto.getFakta().getVurderMilitaer() != null) {
            return new ToggleEndring(null, dto.getFakta().getVurderMilitaer().getHarMilitaer());
        }
        return null;
    }

    private static ToggleEndring utledErSelvstendigNyIArbeidslivetEndring(VurderFaktaOmBeregningDto dto) {
        if (dto.getFakta().getVurderNyIArbeidslivet() != null) {
            return new ToggleEndring(null, dto.getFakta().getVurderNyIArbeidslivet().erNyIArbeidslivet());
        }
        return null;
    }


}
