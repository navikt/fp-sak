package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.datavarehus.xml.BeregningsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.typer.FaktaVurdering;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
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
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BeregningTjeneste beregningTjeneste;

    public BeregningsgrunnlagXmlTjenesteImpl() {
        // For CDI
    }

    @Inject
    public BeregningsgrunnlagXmlTjenesteImpl(FagsakRelasjonRepository fagsakRelasjonRepository,
                                             BeregningTjeneste beregningTjeneste) {
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.beregningTjeneste = beregningTjeneste;
        this.beregningObjectFactory = new ObjectFactory();
    }

    @Override
    public void setBeregningsgrunnlag(Beregningsresultat beregningsresultat, Behandling behandling) {
        var beregningsgrunnlag = beregningObjectFactory.createBeregningsgrunnlagForeldrepenger();
        var fastsattGrunnlag = beregningTjeneste.hent(behandling.getId())
            .filter(bg -> bg.getBeregningsgrunnlagTilstand().equals(BeregningsgrunnlagTilstand.FASTSATT));

        var gjeldendeBg = fastsattGrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        var gjeldendeFakta = fastsattGrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getFaktaAggregat);

        if (gjeldendeBg.isPresent()) {
            var beregningsgrunnlagDomene = gjeldendeBg.get();
            setBeregningsgrunnlagAktivitetStatuser(beregningsgrunnlag, beregningsgrunnlagDomene.getAktivitetStatuser());
            long dekningsgrad = fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak()).getGjeldendeDekningsgrad().getVerdi();
            beregningsgrunnlag.setDekningsgrad(VedtakXmlUtil.lagLongOpplysning(dekningsgrad));
            VedtakXmlUtil.lagDateOpplysning(beregningsgrunnlagDomene.getSkjæringstidspunkt()).ifPresent(beregningsgrunnlag::setSkjaeringstidspunkt);
            setBeregningsgrunnlagPerioder(beregningsgrunnlag, beregningsgrunnlagDomene.getBeregningsgrunnlagPerioder(), gjeldendeFakta);
        }
        var beregningsgrunnlag1 = new Beregningsgrunnlag();
        beregningsgrunnlag1.getAny().add(beregningObjectFactory.createBeregningsgrunnlag(beregningsgrunnlag));
        var json = StandardJsonConfig.toJson(beregningsgrunnlag1);
        beregningsresultat.setBeregningsgrunnlag(beregningsgrunnlag1);
    }

    private void setBeregningsgrunnlagPerioder(BeregningsgrunnlagForeldrepenger beregningsgrunnlag,
                                               List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder,
                                               Optional<FaktaAggregat> gjeldendeFakta) {
        var periodeListe = beregningsgrunnlagPerioder
            .stream()
            .map(p -> konverterFraDomene(p, gjeldendeFakta)).collect(Collectors.toList());
        beregningsgrunnlag.getBeregningsgrunnlagPeriode().addAll(periodeListe);
    }

    private void setBeregningsgrunnlagAktivitetStatuser(BeregningsgrunnlagForeldrepenger beregningsgrunnlag,
                                                        List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser) {
        var aktivitetStatusListe = aktivitetStatuser
            .stream()
            .map(this::konverterFraDomene).collect(Collectors.toList());
        beregningsgrunnlag.getAktivitetstatuser().addAll(aktivitetStatusListe);
    }

    private AktivitetStatus konverterFraDomene(BeregningsgrunnlagAktivitetStatus beregningsgrunnlagAktivitetStatus) {
        var kontrakt = new AktivitetStatus();
        kontrakt.setAktivitetStatus(VedtakXmlUtil.lagKodeverksOpplysning(beregningsgrunnlagAktivitetStatus.getAktivitetStatus()));
        kontrakt.setHjemmel(VedtakXmlUtil.lagKodeverksOpplysning(beregningsgrunnlagAktivitetStatus.getHjemmel()));
        return kontrakt;
    }

    private no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPeriode konverterFraDomene(BeregningsgrunnlagPeriode domene,
                                                                                                                  Optional<FaktaAggregat> gjeldendeFakta) {
        var kontrakt = new no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPeriode();
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(domene.getBeregningsgrunnlagPeriodeFom(), domene.getBeregningsgrunnlagPeriodeTom()));
        kontrakt.setBrutto(lagFloatOpplysning(domene.getBruttoPrÅr()));
        kontrakt.setAvkortet(lagFloatOpplysning(domene.getAvkortetPrÅr()));
        kontrakt.setRedusert(lagFloatOpplysning(domene.getAvkortetPrÅr()));
        Optional.ofNullable(domene.getDagsats()).ifPresent(sats -> kontrakt.setDagsats(VedtakXmlUtil.lagLongOpplysning(sats)));
        setBeregningsgrunnlagPrStatusOgAndel(kontrakt, domene.getBeregningsgrunnlagPrStatusOgAndelList(), gjeldendeFakta);

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
                                                      Optional<FaktaAggregat> gjeldendeFakta) {
        var beregningsgrunnlagPrStatusOgAndelKontrakt = beregningsgrunnlagPrStatusOgAndelList
            .stream()
            .map(a -> konverterFraDomene(a, gjeldendeFakta)).collect(Collectors.toList());
        kontrakt.getBeregningsgrunnlagPrStatusOgAndel().addAll(beregningsgrunnlagPrStatusOgAndelKontrakt);
    }

    private no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPrStatusOgAndel konverterFraDomene(
        BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel,
        Optional<FaktaAggregat> gjeldendeFakta) {
        var tidsbegrensetVurdering = beregningsgrunnlagPrStatusOgAndel.getBgAndelArbeidsforhold()
            .flatMap(arb -> gjeldendeFakta.flatMap(fakta -> fakta.getFaktaArbeidsforhold(arb)))
            .map(FaktaArbeidsforhold::getErTidsbegrenset).map(FaktaVurdering::getVurdering);
        var kontrakt = new no.nav.vedtak.felles.xml.vedtak.beregningsgrunnlag.fp.v2.BeregningsgrunnlagPrStatusOgAndel();
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(beregningsgrunnlagPrStatusOgAndel.getBeregningsperiodeFom(),
            beregningsgrunnlagPrStatusOgAndel.getBeregningsperiodeTom()));
        kontrakt.setAktivitetstatus(VedtakXmlUtil.lagKodeverksOpplysning(beregningsgrunnlagPrStatusOgAndel.getAktivitetStatus()));
        beregningsgrunnlagPrStatusOgAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver)
            .ifPresent(arbeidsgiver -> kontrakt.setVirksomhetsnummer(VedtakXmlUtil.lagStringOpplysning(arbeidsgiver.getIdentifikator())));

        kontrakt.setErTidsbegrensetArbeidsforhold(VedtakXmlUtil.lagBooleanOpplysning(tidsbegrensetVurdering.orElse(null)));
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
