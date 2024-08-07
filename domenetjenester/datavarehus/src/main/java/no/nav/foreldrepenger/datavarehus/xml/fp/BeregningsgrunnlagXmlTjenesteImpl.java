package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.xml.felles.v2.FloatOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.LongOpplysning;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.AktivitetStatus;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.GjennomsnittligPensjonsgivendeInntekt;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.RefusjonTilArbeidsgiver;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsgrunnlag;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class BeregningsgrunnlagXmlTjenesteImpl implements BeregningsgrunnlagXmlTjeneste {

    private ObjectFactory beregningObjectFactory;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private BeregningTjeneste beregningTjeneste;

    public BeregningsgrunnlagXmlTjenesteImpl() {
        // For CDI
    }

    @Inject
    public BeregningsgrunnlagXmlTjenesteImpl(DekningsgradTjeneste dekningsgradTjeneste,
                                             BeregningTjeneste beregningTjeneste) {
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.beregningObjectFactory = new ObjectFactory();
    }

    @Override
    public void setBeregningsgrunnlag(Beregningsresultat beregningsresultat, Behandling behandling) {
        var beregningsgrunnlagXml = beregningObjectFactory.createBeregningsgrunnlagForeldrepenger();
        var bgAggregat = beregningTjeneste.hent(BehandlingReferanse.fra(behandling))
            .filter(bg -> bg.getBeregningsgrunnlagTilstand().equals(BeregningsgrunnlagTilstand.FASTSATT));
        var beregningsgrunnlag = bgAggregat.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);

        if (beregningsgrunnlag.isPresent()) {
            var beregningsgrunnlagDomene = beregningsgrunnlag.get();
            var faktaAggregat = bgAggregat.flatMap(BeregningsgrunnlagGrunnlag::getFaktaAggregat);
            setBeregningsgrunnlagAktivitetStatuser(beregningsgrunnlagXml, beregningsgrunnlagDomene.getAktivitetStatuser());
            var dekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(BehandlingReferanse.fra(behandling)).getVerdi();
            beregningsgrunnlagXml.setDekningsgrad(VedtakXmlUtil.lagLongOpplysning(dekningsgrad));
            VedtakXmlUtil.lagDateOpplysning(beregningsgrunnlagDomene.getSkjæringstidspunkt()).ifPresent(beregningsgrunnlagXml::setSkjaeringstidspunkt);
            setBeregningsgrunnlagPerioder(beregningsgrunnlagXml, beregningsgrunnlagDomene.getBeregningsgrunnlagPerioder(), faktaAggregat);
        }
        var beregningsgrunnlag1 = new Beregningsgrunnlag();
        beregningsgrunnlag1.getAny().add(beregningObjectFactory.createBeregningsgrunnlag(beregningsgrunnlagXml));
        beregningsresultat.setBeregningsgrunnlag(beregningsgrunnlag1);
    }

    private void setBeregningsgrunnlagPerioder(BeregningsgrunnlagForeldrepenger beregningsgrunnlag,
                                               List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder, Optional<FaktaAggregat> faktaAggregat) {
        var periodeListe = beregningsgrunnlagPerioder
            .stream()
            .map(p -> konverterFraDomene(p, faktaAggregat)).toList();
        beregningsgrunnlag.getBeregningsgrunnlagPeriode().addAll(periodeListe);
    }

    private void setBeregningsgrunnlagAktivitetStatuser(BeregningsgrunnlagForeldrepenger beregningsgrunnlag,
                                                        List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        var aktivitetStatusListe = aktivitetStatuser
            .stream()
            .map(this::konverterFraDomene).toList();
        beregningsgrunnlag.getAktivitetstatuser().addAll(aktivitetStatusListe);
    }

    private AktivitetStatus konverterFraDomene(BeregningsgrunnlagAktivitetStatus beregningsgrunnlagAktivitetStatus) {
        var kontrakt = new AktivitetStatus();
        kontrakt.setAktivitetStatus(VedtakXmlUtil.lagKodeverksOpplysning(beregningsgrunnlagAktivitetStatus.getAktivitetStatus()));
        kontrakt.setHjemmel(VedtakXmlUtil.lagKodeverksOpplysning(beregningsgrunnlagAktivitetStatus.getHjemmel()));
        return kontrakt;
    }

    private no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPeriode konverterFraDomene(BeregningsgrunnlagPeriode domene,
                                                                                                                  Optional<FaktaAggregat> faktaAggregat) {
        var kontrakt = new no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPeriode();
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(domene.getBeregningsgrunnlagPeriodeFom(), domene.getBeregningsgrunnlagPeriodeTom()));
        kontrakt.setBrutto(lagFloatOpplysning(domene.getBruttoPrÅr()));
        kontrakt.setAvkortet(lagFloatOpplysning(domene.getAvkortetPrÅr()));
        kontrakt.setRedusert(lagFloatOpplysning(domene.getAvkortetPrÅr()));
        Optional.ofNullable(domene.getDagsats()).ifPresent(sats -> kontrakt.setDagsats(VedtakXmlUtil.lagLongOpplysning(sats)));
        setBeregningsgrunnlagPrStatusOgAndel(kontrakt, domene.getBeregningsgrunnlagPrStatusOgAndelList(), faktaAggregat);

        return kontrakt;
    }

    private FloatOpplysning lagFloatOpplysning(BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            return null;
        }
        return VedtakXmlUtil.lagFloatOpplysning(bigDecimal.floatValue());
    }

    private LongOpplysning lagLongOpplysning(BigDecimal bigDecimal) {
        if (bigDecimal == null) {
            return null;
        }
        return VedtakXmlUtil.lagLongOpplysning(bigDecimal.longValue());
    }

    private void setBeregningsgrunnlagPrStatusOgAndel(no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPeriode kontrakt,
                                                      List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList,
                                                      Optional<FaktaAggregat> faktaAggregat) {
        var beregningsgrunnlagPrStatusOgAndelKontrakt = beregningsgrunnlagPrStatusOgAndelList
            .stream()
            .map(a -> konverterFraDomene(a,  faktaAggregat))
            .toList();
        kontrakt.getBeregningsgrunnlagPrStatusOgAndel().addAll(beregningsgrunnlagPrStatusOgAndelKontrakt);
    }

    private no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPrStatusOgAndel konverterFraDomene(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel, Optional<FaktaAggregat> faktaAggregat) {
        var kontrakt = new no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPrStatusOgAndel();
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(beregningsgrunnlagPrStatusOgAndel.getBeregningsperiodeFom(),
            beregningsgrunnlagPrStatusOgAndel.getBeregningsperiodeTom()));
        kontrakt.setAktivitetstatus(VedtakXmlUtil.lagKodeverksOpplysning(beregningsgrunnlagPrStatusOgAndel.getAktivitetStatus()));
        beregningsgrunnlagPrStatusOgAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver)
            .ifPresent(arbeidsgiver -> kontrakt.setVirksomhetsnummer(VedtakXmlUtil.lagStringOpplysning(arbeidsgiver.getIdentifikator())));
        kontrakt.setErTidsbegrensetArbeidsforhold(VedtakXmlUtil.lagBooleanOpplysning(erTidsbegrenset(beregningsgrunnlagPrStatusOgAndel, faktaAggregat)
            .map(FaktaArbeidsforhold::getErTidsbegrensetVurdering).orElse(null)));
        kontrakt.setBrutto(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getBruttoPrÅr()));
        kontrakt.setAvkortet(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getAvkortetPrÅr()));
        kontrakt.setRedusert(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getRedusertPrÅr()));
        kontrakt.setOverstyrt(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getOverstyrtPrÅr()));
        kontrakt.setInntektskategori(VedtakXmlUtil.lagKodeverksOpplysning(beregningsgrunnlagPrStatusOgAndel.getGjeldendeInntektskategori()));
        kontrakt.setRefusjonTilArbeidsgiver(convertRefusjonTilArbeidsgiverFraDomene(beregningsgrunnlagPrStatusOgAndel));
        beregningsgrunnlagPrStatusOgAndel.getBgAndelArbeidsforhold().flatMap(BGAndelArbeidsforhold::getNaturalytelseBortfaltPrÅr)
            .ifPresent(nybpå -> kontrakt.setNaturalytelseBortfall(VedtakXmlUtil.lagFloatOpplysning(nybpå.floatValue())));
        kontrakt.setBeregnet(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getBeregnetPrÅr()));
        kontrakt.setGjennomsnittligPensjonsgivendeInntekt(konverterGjennomsnittligPensjonsgivendeInntektFraDomene(beregningsgrunnlagPrStatusOgAndel));
        kontrakt.setTilstoetendeYtelseType(VedtakXmlUtil.lagKodeverksOpplysning(RelatertYtelseType.UDEFINERT));
        kontrakt.setTilstoetendeYtelse(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getÅrsbeløpFraTilstøtendeYtelseVerdi()));
        kontrakt.setAvkortetBrukersAndel(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getAvkortetBrukersAndelPrÅr()));
        kontrakt.setRedusertBrukersAndel(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getRedusertBrukersAndelPrÅr()));
        kontrakt.setDagsatsBruker(VedtakXmlUtil.lagLongOpplysning(beregningsgrunnlagPrStatusOgAndel.getDagsatsBruker()));
        kontrakt.setDagsatsArbeidsgiver(VedtakXmlUtil.lagLongOpplysning(beregningsgrunnlagPrStatusOgAndel.getDagsatsArbeidsgiver()));

        return kontrakt;

    }

    private Optional<FaktaArbeidsforhold> erTidsbegrenset(BeregningsgrunnlagPrStatusOgAndel andel, Optional<FaktaAggregat> faktaAggregat) {
        if (andel.getBgAndelArbeidsforhold().isEmpty() || !andel.getAktivitetStatus().erArbeidstaker()) {
            return Optional.empty();
        }
        var faktaArbeidsforholds = faktaAggregat.map(FaktaAggregat::getFaktaArbeidsforhold).orElse(Collections.emptyList());
        return faktaArbeidsforholds.stream()
            .filter(a -> a.gjelderFor(andel.getArbeidsgiver().orElse(null), andel.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef())))
            .findFirst();
    }

    private GjennomsnittligPensjonsgivendeInntekt konverterGjennomsnittligPensjonsgivendeInntektFraDomene(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
        var gjennomsnittligPensjonsgivendeInntekt = new GjennomsnittligPensjonsgivendeInntekt();
        gjennomsnittligPensjonsgivendeInntekt.setPgisnitt(lagLongOpplysning(beregningsgrunnlagPrStatusOgAndel.getPgiSnitt()));
        gjennomsnittligPensjonsgivendeInntekt.setPgiaar1(lagLongOpplysning(beregningsgrunnlagPrStatusOgAndel.getPgi1()));
        gjennomsnittligPensjonsgivendeInntekt.setPgiaar2(lagLongOpplysning(beregningsgrunnlagPrStatusOgAndel.getPgi2()));
        gjennomsnittligPensjonsgivendeInntekt.setPgiaar3(lagLongOpplysning(beregningsgrunnlagPrStatusOgAndel.getPgi3()));
        return gjennomsnittligPensjonsgivendeInntekt;

    }

    private RefusjonTilArbeidsgiver convertRefusjonTilArbeidsgiverFraDomene(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
        var refusjonTilArbeidsgiver = new RefusjonTilArbeidsgiver();
        refusjonTilArbeidsgiver.setRefusjonskrav(
            lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(null)));
        refusjonTilArbeidsgiver.setMaksimal(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getMaksimalRefusjonPrÅr()));
        refusjonTilArbeidsgiver.setAvkortet(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getAvkortetRefusjonPrÅr()));
        refusjonTilArbeidsgiver.setRedusert(lagFloatOpplysning(beregningsgrunnlagPrStatusOgAndel.getRedusertRefusjonPrÅr()));
        return refusjonTilArbeidsgiver;
    }
}
