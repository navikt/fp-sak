package no.nav.foreldrepenger.web.app.tjenester;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.domene.opptjening.dto.AvklarAktivitetsPerioderDto;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.rest.dto.VurderFaktaOmBeregningDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.omsorgsovertakelse.dto.VurderOmsorgsovertakelseVilkårAksjonspunktDto;
import no.nav.foreldrepenger.web.app.IndexClasses;
import no.nav.foreldrepenger.web.app.tjenester.abakus.IAYRegisterdataCallbackRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjenestePathHack1;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.AnkeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding.ArbeidOgInntektsmeldingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.FeriepengegrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.FødselOmsorgsovertakelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.InnsynRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapspengerRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.OppgaverRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelsefordelingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.AktørRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.FormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBatchRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBehandlingskontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBeregningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningFagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningLagretVedtakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningOppdragRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningOpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningStegRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningSvangerskapspengerRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningSøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningTekniskRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningUttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningUttrekkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt.FpoversiktMigreringRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk.ForvaltningStønadsstatistikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fpoversikt.FpOversiktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.infotrygd.InfotrygdOppslagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.los.LosRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.register.RedirectToRegisterRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.InitielleLinksRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.VedtakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.VedtakJsonFeedRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.PipRestTjeneste;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

public class RestImplementationClasses {

    private RestImplementationClasses() {
    }

    public static Collection<Class<?>> getImplementationClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Frontend
        classes.add(InitielleLinksRestTjeneste.class);
        classes.add(FagsakRestTjeneste.class);
        classes.add(BehandlingRestTjeneste.class);
        classes.add(BehandlingRestTjenestePathHack1.class);
        classes.add(BeregningsgrunnlagRestTjeneste.class);
        classes.add(AksjonspunktRestTjeneste.class);
        classes.add(DokumentRestTjeneste.class);
        classes.add(KodeverkRestTjeneste.class);
        classes.add(HistorikkRestTjeneste.class);
        classes.add(UttakRestTjeneste.class);
        classes.add(BeregningsresultatRestTjeneste.class);
        classes.add(FeriepengegrunnlagRestTjeneste.class);
        classes.add(VedtakRestTjeneste.class);
        classes.add(PersonRestTjeneste.class);
        classes.add(YtelsefordelingRestTjeneste.class);
        classes.add(SøknadRestTjeneste.class);
        classes.add(OpptjeningRestTjeneste.class);
        classes.add(InntektArbeidYtelseRestTjeneste.class);
        classes.add(ArbeidOgInntektsmeldingRestTjeneste.class);
        classes.add(FamiliehendelseRestTjeneste.class);
        classes.add(KlageRestTjeneste.class);
        classes.add(AnkeRestTjeneste.class);
        classes.add(InnsynRestTjeneste.class);
        classes.add(TilbakekrevingRestTjeneste.class); // Brukes av Frontend/Simulering. Bør hente varseltekst til visning?
        classes.add(AktørRestTjeneste.class);
        classes.add(SvangerskapspengerRestTjeneste.class);
        classes.add(VergeRestTjeneste.class);
        classes.add(BrevRestTjeneste.class);
        classes.add(RedirectToRegisterRestTjeneste.class);
        classes.add(OppgaverRestTjeneste.class);
        classes.add(FødselOmsorgsovertakelseRestTjeneste.class);

        // Søk infotrygd
        classes.add(InfotrygdOppslagRestTjeneste.class);

        return Set.copyOf(classes);
    }

    public static Collection<Class<?>> getServiceClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(VedtakJsonFeedRestTjeneste.class); // Infotrygd og Arena
        classes.add(PipRestTjeneste.class); // FPtilgang og SAF og Kabal
        classes.add(IAYRegisterdataCallbackRestTjeneste.class); // FPabakus
        classes.add(FordelRestTjeneste.class); // FPfordel
        classes.add(HendelserRestTjeneste.class); // FPabonnent
        classes.add(LosRestTjeneste.class); // FPlos
        classes.add(TilbakeRestTjeneste.class); // FPtilbake
        classes.add(FpOversiktRestTjeneste.class); // FPoversikt
        classes.add(FormidlingRestTjeneste.class); // FPformidling

        return Set.copyOf(classes);
    }

    public static Set<Class<?>> getForvaltningClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // UtilTjenester for uttrekk fra registre
        classes.add(ProsessTaskRestTjeneste.class);
        classes.add(ForvaltningBatchRestTjeneste.class);
        classes.add(ForvaltningLagretVedtakRestTjeneste.class);
        classes.add(ForvaltningBehandlingskontrollRestTjeneste.class);
        classes.add(ForvaltningBeregningRestTjeneste.class);
        classes.add(ForvaltningFagsakRestTjeneste.class);
        classes.add(ForvaltningTekniskRestTjeneste.class);
        classes.add(ForvaltningUttrekkRestTjeneste.class);
        classes.add(ForvaltningOppdragRestTjeneste.class);
        classes.add(ForvaltningOpptjeningRestTjeneste.class);
        classes.add(ForvaltningUttakRestTjeneste.class);
        classes.add(ForvaltningStønadsstatistikkRestTjeneste.class);
        classes.add(ForvaltningBehandlingRestTjeneste.class);
        classes.add(ForvaltningStegRestTjeneste.class);
        classes.add(ForvaltningSvangerskapspengerRestTjeneste.class);
        classes.add(ForvaltningSøknadRestTjeneste.class);
        classes.add(FpoversiktMigreringRestTjeneste.class);

        return Collections.unmodifiableSet(classes);
    }

    public static Set<Class<?>> allJsonTypeNameClasses() {
        final var scanClasses = new LinkedHashSet<>(getImplementationClasses());

        scanClasses.add(AvklarAktivitetsPerioderDto.class);
        scanClasses.add(VurderFaktaOmBeregningDto.class);
        scanClasses.add(VurderOmsorgsovertakelseVilkårAksjonspunktDto.class);
        scanClasses.add(AvklarVergeDto.class);

        return scanClasses.stream()
            .map(c -> {
                try {
                    return c.getProtectionDomain().getCodeSource().getLocation().toURI();
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Ikke en URI for klasse: " + c, e);
                }
            })
            .distinct()
            .flatMap(uri -> getJsonTypeNameClasses(uri).stream())
            .collect(Collectors.toUnmodifiableSet());

    }

    /**
     * Scan subtyper dynamisk fra WAR slik at superklasse slipper å
     * deklarere @JsonSubtypes.
     */
    public static List<Class<?>> getJsonTypeNameClasses(URI classLocation) {
        IndexClasses indexClasses;
        indexClasses = IndexClasses.getIndexFor(classLocation);
        return indexClasses.getClassesWithAnnotation(JsonTypeName.class);
    }
}
