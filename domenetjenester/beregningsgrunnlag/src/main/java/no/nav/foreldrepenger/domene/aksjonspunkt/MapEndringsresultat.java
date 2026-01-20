package no.nav.foreldrepenger.domene.aksjonspunkt;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus.KodeverkFraKalkulusMapper;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.OppdateringRespons;
import no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.VarigEndretEllerNyoppstartetNæringEndring;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Aktør;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Beløp;

public class MapEndringsresultat {

    private MapEndringsresultat() {
        // Skjul
    }

    public static OppdaterBeregningsgrunnlagResultat mapFraOppdateringRespons(OppdateringRespons oppdateringRespons) {
        return oppdateringRespons == null ? null :
            new OppdaterBeregningsgrunnlagResultat(mapTilBeregningsgrunnlagEndring(oppdateringRespons.getBeregningsgrunnlagEndring()),
                mapFaktaOmBeregningVurderinger(oppdateringRespons.getFaktaOmBeregningVurderinger()),
                mapVarigEndretNæringVurdering(oppdateringRespons.getVarigEndretNæringEndring()),
                mapEndringRefusjon(oppdateringRespons.getRefusjonoverstyringEndring()),
                mapBeregningAktivitetEndringer(oppdateringRespons.getBeregningAktiviteterEndring()));
    }

    private static List<no.nav.foreldrepenger.domene.aksjonspunkt.BeregningAktivitetEndring> mapBeregningAktivitetEndringer(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningAktiviteterEndring beregningAktiviteterEndring) {
        return beregningAktiviteterEndring == null ? Collections.emptyList() :
            beregningAktiviteterEndring.getAktivitetEndringer().stream().map(MapEndringsresultat::mapTilAktivitetEndring).toList();
    }

    private static no.nav.foreldrepenger.domene.aksjonspunkt.BeregningAktivitetEndring mapTilAktivitetEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningAktivitetEndring beregningAktivitetEndring) {
        return new no.nav.foreldrepenger.domene.aksjonspunkt.BeregningAktivitetEndring(
            mapNøkkel(beregningAktivitetEndring.getAktivitetNøkkel()),
            mapTilToggle(beregningAktivitetEndring.getSkalBrukesEndring()),
            mapDatoEndring(beregningAktivitetEndring.getTomDatoEndring())
        );
    }

    private static BeregningAktivitetNøkkel mapNøkkel(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningAktivitetNøkkel aktivitetNøkkel) {
        return new BeregningAktivitetNøkkel(
            OpptjeningAktivitetType.fraKode(aktivitetNøkkel.getOpptjeningAktivitetType().getKode()),
            aktivitetNøkkel.getFom(),
            mapArbeidsgiver(aktivitetNøkkel.getArbeidsgiver()),
            aktivitetNøkkel.getArbeidsforholdRef() != null ? mapArbeidsforholdRef(aktivitetNøkkel.getArbeidsforholdRef().getAbakusReferanse()) : InternArbeidsforholdRef.nullRef());
    }

    private static no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonoverstyringEndring mapEndringRefusjon(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.RefusjonoverstyringEndring refusjonoverstyringEndring) {
        if (refusjonoverstyringEndring == null) {
            return null;
        }
        return new no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonoverstyringEndring(
            mapRefusjonOverstyringPerioder(refusjonoverstyringEndring.getRefusjonperiodeEndringer())
        );
    }

    private static List<RefusjonoverstyringPeriodeEndring> mapRefusjonOverstyringPerioder(List<no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.RefusjonoverstyringPeriodeEndring> refusjonperiodeEndringer) {
        if (refusjonperiodeEndringer == null) {
            return null;
        }
        return refusjonperiodeEndringer.stream().map(MapEndringsresultat::mapRefusjonPeriodeEndring).toList();
    }

    private static RefusjonoverstyringPeriodeEndring mapRefusjonPeriodeEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.RefusjonoverstyringPeriodeEndring r) {
        return new RefusjonoverstyringPeriodeEndring(
            mapArbeidsgiver(r.getArbeidsgiver()),
            mapArbeidsforholdRef(Optional.ofNullable(r.getArbeidsforholdRef()).map(UUID::toString).orElse(null)),
            mapDatoEndring(r.getFastsattRefusjonFomEndring()),
            r.getFastsattDelvisRefusjonFørDatoEndring() == null ? null :
                new BeløpEndring(Beløp.safeVerdi(r.getFastsattDelvisRefusjonFørDatoEndring().getFraRefusjon()),
                    Beløp.safeVerdi(r.getFastsattDelvisRefusjonFørDatoEndring().getTilRefusjon()))
        );
    }

    private static DatoEndring mapDatoEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.DatoEndring datoEndring) {
        return datoEndring == null ? null : new DatoEndring(datoEndring.getFraVerdi(), datoEndring.getTilVerdi());
    }

    private static VarigEndretNæringVurdering mapVarigEndretNæringVurdering(VarigEndretEllerNyoppstartetNæringEndring varigEndretEllerNyoppstartetNæringEndring) {
        if (harVerdier(varigEndretEllerNyoppstartetNæringEndring)) {
            return new VarigEndretNæringVurdering(
                mapTilToggle(varigEndretEllerNyoppstartetNæringEndring.getErVarigEndretNaeringEndring()),
                mapTilToggle(varigEndretEllerNyoppstartetNæringEndring.getErNyoppstartetNaeringEndring()));
        }
        return null;
    }

    private static boolean harVerdier(VarigEndretEllerNyoppstartetNæringEndring varigEndretEllerNyoppstartetNæringEndring) {
        return varigEndretEllerNyoppstartetNæringEndring != null && (varigEndretEllerNyoppstartetNæringEndring.getErVarigEndretNaeringEndring() != null || varigEndretEllerNyoppstartetNæringEndring.getErNyoppstartetNaeringEndring() != null);
    }

    private static FaktaOmBeregningVurderinger mapFaktaOmBeregningVurderinger(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.FaktaOmBeregningVurderinger faktaOmBeregningVurderinger) {
        if (faktaOmBeregningVurderinger == null) {
            return null;
        }
        FaktaOmBeregningVurderinger vurderinger = new FaktaOmBeregningVurderinger();
        vurderinger.setHarEtterlønnSluttpakkeEndring(mapTilToggle(faktaOmBeregningVurderinger.getHarEtterlønnSluttpakkeEndring()));
        vurderinger.setHarLønnsendringIBeregningsperiodenEndring(mapTilToggle(faktaOmBeregningVurderinger.getHarLønnsendringIBeregningsperiodenEndring()));
        vurderinger.setHarMilitærSiviltjenesteEndring(mapTilToggle(faktaOmBeregningVurderinger.getHarMilitærSiviltjenesteEndring()));
        vurderinger.setErSelvstendingNyIArbeidslivetEndring(mapTilToggle(faktaOmBeregningVurderinger.getErSelvstendingNyIArbeidslivetEndring()));
        vurderinger.setErNyoppstartetFLEndring(mapTilToggle(faktaOmBeregningVurderinger.getErNyoppstartetFLEndring()));
        vurderinger.setErMottattYtelseEndringer(mapTilErMottattYtelseEndringer(faktaOmBeregningVurderinger.getErMottattYtelseEndringer()));
        vurderinger.setErTidsbegrensetArbeidsforholdEndringer(mapTilErTidsbegrensetArbeidsforholdEndringer(faktaOmBeregningVurderinger.getErTidsbegrensetArbeidsforholdEndringer()));
        vurderinger.setVurderRefusjonskravGyldighetEndringer(mapTilRefusjonskravGyldighetEndringer(faktaOmBeregningVurderinger.getVurderRefusjonskravGyldighetEndringer()));
        return vurderinger;
    }

    private static List<ErTidsbegrensetArbeidsforholdEndring> mapTilErTidsbegrensetArbeidsforholdEndringer(List<no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.ErTidsbegrensetArbeidsforholdEndring> erTidsbegrensetArbeidsforholdEndringer) {
        return erTidsbegrensetArbeidsforholdEndringer == null ? null :
            erTidsbegrensetArbeidsforholdEndringer.stream()
                .map(MapEndringsresultat::mapErTidsbegrensetArbeidsforholdEndring)
                .toList();
    }

    private static ErTidsbegrensetArbeidsforholdEndring mapErTidsbegrensetArbeidsforholdEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.ErTidsbegrensetArbeidsforholdEndring erTidsbegrensetArbeidsforholdEndring) {
        return erTidsbegrensetArbeidsforholdEndring == null ? null :
            new ErTidsbegrensetArbeidsforholdEndring(
                mapArbeidsgiver(erTidsbegrensetArbeidsforholdEndring.getArbeidsgiver()),
                mapArbeidsforholdRef(Optional.ofNullable(erTidsbegrensetArbeidsforholdEndring.getArbeidsforholdRef()).map(UUID::toString).orElse(null)),
                mapTilToggle(erTidsbegrensetArbeidsforholdEndring.getErTidsbegrensetArbeidsforholdEndring())
            );
    }

    private static List<ErMottattYtelseEndring> mapTilErMottattYtelseEndringer(List<no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.ErMottattYtelseEndring> erMottattYtelseEndringer) {
        return erMottattYtelseEndringer == null ? null : erMottattYtelseEndringer.stream()
            .map(MapEndringsresultat::mapErMottattYtelseEndring)
            .toList();
    }

    private static ErMottattYtelseEndring mapErMottattYtelseEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.ErMottattYtelseEndring erMottattYtelseEndring) {
        if (erMottattYtelseEndring == null) {
            return null;
        }
        return new ErMottattYtelseEndring(
            AktivitetStatus.fraKode(erMottattYtelseEndring.getAktivitetStatus().getKode()),
            mapArbeidsgiver(erMottattYtelseEndring.getArbeidsgiver()),
            mapArbeidsforholdRef(Optional.ofNullable(erMottattYtelseEndring.getArbeidsforholdRef()).map(UUID::toString).orElse(null)),
            mapTilToggle(erMottattYtelseEndring.getErMottattYtelseEndring())
        );
    }

    private static List<RefusjonskravGyldighetEndring> mapTilRefusjonskravGyldighetEndringer(List<no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.RefusjonskravGyldighetEndring> vurderRefusjonskravGyldighetEndringer) {
        return vurderRefusjonskravGyldighetEndringer == null ? null : vurderRefusjonskravGyldighetEndringer.stream().map(MapEndringsresultat::mapRefusjonskravGyldighetEndring)
            .toList();
    }

    private static RefusjonskravGyldighetEndring mapRefusjonskravGyldighetEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.RefusjonskravGyldighetEndring refusjonskravGyldighetEndring) {
        return refusjonskravGyldighetEndring == null ? null :
            new RefusjonskravGyldighetEndring(new ToggleEndring(refusjonskravGyldighetEndring.getErGyldighetUtvidet().getFraVerdi(), refusjonskravGyldighetEndring.getErGyldighetUtvidet().getTilVerdi()),
                refusjonskravGyldighetEndring.getArbeidsgiver().getErOrganisasjon() ? Arbeidsgiver.virksomhet(refusjonskravGyldighetEndring.getArbeidsgiver().getIdent()) :
                    Arbeidsgiver.person(new AktørId(refusjonskravGyldighetEndring.getArbeidsgiver().getIdent())));
    }

    private static ToggleEndring mapTilToggle(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.ToggleEndring toggleEndring) {
        return toggleEndring == null ? null : new ToggleEndring(toggleEndring.getFraVerdi(), toggleEndring.getTilVerdi());
    }


    private static BeregningsgrunnlagEndring mapTilBeregningsgrunnlagEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningsgrunnlagEndring beregningsgrunnlagEndring) {
        return beregningsgrunnlagEndring == null ? null :
            new BeregningsgrunnlagEndring(mapTilPeriodeEndringer(beregningsgrunnlagEndring.getBeregningsgrunnlagPeriodeEndringer()));
    }

    private static List<BeregningsgrunnlagPeriodeEndring> mapTilPeriodeEndringer(List<no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningsgrunnlagPeriodeEndring> beregningsgrunnlagPeriodeEndringer) {
        return beregningsgrunnlagPeriodeEndringer == null ? null :
            beregningsgrunnlagPeriodeEndringer.stream().map(MapEndringsresultat::mapTilPeriodeEndring).toList();
    }

    private static BeregningsgrunnlagPeriodeEndring mapTilPeriodeEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningsgrunnlagPeriodeEndring beregningsgrunnlagPeriodeEndring) {
        return new BeregningsgrunnlagPeriodeEndring(
            mapAndelEndringer(beregningsgrunnlagPeriodeEndring.getBeregningsgrunnlagPrStatusOgAndelEndringer()),
            DatoIntervallEntitet.fraOgMedTilOgMed(beregningsgrunnlagPeriodeEndring.getPeriode().getFom(), beregningsgrunnlagPeriodeEndring.getPeriode().getTom())
        );
    }

    private static List<BeregningsgrunnlagPrStatusOgAndelEndring> mapAndelEndringer(List<no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningsgrunnlagPrStatusOgAndelEndring> beregningsgrunnlagPrStatusOgAndelEndringer) {
        return beregningsgrunnlagPrStatusOgAndelEndringer.stream().map(MapEndringsresultat::mapAndelEndring).toList();
    }

    private static BeregningsgrunnlagPrStatusOgAndelEndring mapAndelEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.BeregningsgrunnlagPrStatusOgAndelEndring beregningsgrunnlagPrStatusOgAndelEndring) {
        return new BeregningsgrunnlagPrStatusOgAndelEndring(
            mapInntektEndring(beregningsgrunnlagPrStatusOgAndelEndring.getInntektEndring()),
            mapInntektskategoriEndring(beregningsgrunnlagPrStatusOgAndelEndring.getInntektskategoriEndring()),
            mapRefusjonEndring(beregningsgrunnlagPrStatusOgAndelEndring.getRefusjonEndring()),
            AktivitetStatus.fraKode(beregningsgrunnlagPrStatusOgAndelEndring.getAktivitetStatus().getKode()),
            beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdType() == null ? null : OpptjeningAktivitetType.fraKode(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdType().getKode()),
            mapArbeidsgiver(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsgiver()),
            mapArbeidsforholdRef(Optional.ofNullable(beregningsgrunnlagPrStatusOgAndelEndring.getArbeidsforholdRef()).map(UUID::toString).orElse(null))
        );
    }

    private static RefusjonEndring mapRefusjonEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.RefusjonEndring refusjonEndring) {
        return refusjonEndring == null ? null : new RefusjonEndring(Beløp.safeVerdi(refusjonEndring.getFraRefusjon()),
            Beløp.safeVerdi(refusjonEndring.getTilRefusjon()));
    }

    private static InternArbeidsforholdRef mapArbeidsforholdRef(String arbeidsforholdRef) {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : InternArbeidsforholdRef.ref(arbeidsforholdRef);
    }

    private static Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErOrganisasjon() ? Arbeidsgiver.virksomhet(arbeidsgiver.getIdent()) : Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
    }

    private static InntektskategoriEndring mapInntektskategoriEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.InntektskategoriEndring inntektskategoriEndring) {
        return inntektskategoriEndring == null ? null : new InntektskategoriEndring(
            inntektskategoriEndring.getFraVerdi() == null ? null : KodeverkFraKalkulusMapper.mapInntektskategori(inntektskategoriEndring.getFraVerdi()),
            KodeverkFraKalkulusMapper.mapInntektskategori(inntektskategoriEndring.getTilVerdi()));
    }

    private static BeløpEndring mapInntektEndring(no.nav.foreldrepenger.kalkulus.kontrakt.response.håndtering.InntektEndring inntektEndring) {
        return inntektEndring == null ? null : new BeløpEndring(Beløp.safeVerdi(inntektEndring.getFraInntekt()),
            Beløp.safeVerdi(inntektEndring.getTilInntekt()));
    }


}
