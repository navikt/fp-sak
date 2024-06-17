package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.entiteter.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT)
public class VurderRefusjonHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;

    VurderRefusjonHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public VurderRefusjonHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag) {
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
    }

    @Override
    public void lagHistorikk(Long behandlingId,
                             FaktaBeregningLagreDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                             Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag,
                             InntektArbeidYtelseGrunnlag iayGrunnlag) {
        for (var vurderingDto : dto.getRefusjonskravGyldighet()) {
            var arbeidsgiver = finnArbeidsgiver(vurderingDto.getArbeidsgiverId());
            var frist = nyttBeregningsgrunnlag.getSkjæringstidspunkt();
            var forrige = finnForrigeVerdi(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getRefusjonOverstyringer), arbeidsgiver, frist);
            lagHistorikkInnslag(Boolean.TRUE.equals(vurderingDto.isSkalUtvideGyldighet()), forrige,
                arbeidsgiverHistorikkinnslag.lagTekstForArbeidsgiver(arbeidsgiver, iayGrunnlag.getArbeidsforholdOverstyringer()), tekstBuilder);
        }
    }

    private Boolean finnForrigeVerdi(Optional<BeregningRefusjonOverstyringerEntitet> forrigeBeregningRefusjonOverstyringer,
                                     Arbeidsgiver arbeidsgiver,
                                     LocalDate frist) {
        return forrigeBeregningRefusjonOverstyringer.map(BeregningRefusjonOverstyringerEntitet::getRefusjonOverstyringer)
            .orElse(Collections.emptyList())
            .stream()
            .filter(beregningRefusjonOverstyring -> beregningRefusjonOverstyring.getArbeidsgiver().equals(arbeidsgiver))
            .findFirst()
            .map(beregningRefusjonOverstyring -> Objects.equals(beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom().orElse(null), frist))
            .orElse(null);
    }

    private Arbeidsgiver finnArbeidsgiver(String identifikator) {
        if (OrgNummer.erGyldigOrgnr(identifikator)) {
            return Arbeidsgiver.virksomhet(identifikator);
        }
        return Arbeidsgiver.fra(new AktørId(identifikator));
    }

    private void lagHistorikkInnslag(Boolean nyVerdi, Boolean forrige, String arbeidsgivernavn, HistorikkInnslagTekstBuilder tekstBuilder) {
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.NY_REFUSJONSFRIST, arbeidsgivernavn, forrige, nyVerdi);
    }

}
