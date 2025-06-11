package no.nav.foreldrepenger.web.app.tjenester;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.foreldrepenger.web.app.tjenester.abakus.IAYRegisterdataCallbackRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.batch.BatchRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingBackendRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjenestePathHack1;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.AnkeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding.ArbeidOgInntektsmeldingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.FeriepengegrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.FødselRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.InnsynRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag.OppdragRestTjeneste;
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
import no.nav.foreldrepenger.web.app.tjenester.datavarehus.DatavarehusAdminRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.AktoerRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.FormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.arbeidsforholdInntektsmelding.ArbeidsforholdInntektsmeldingFormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.BeregningsgrunnlagFormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.formidling.tilkjentytelse.TilkjentYtelseFormidlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBehandlingskontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningBeregningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningFagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningOppdragRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningOpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningStegRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.stonadsstatistikk.ForvaltningStønadsstatistikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningSvangerskapspengerRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningSøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningTekniskRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningUttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.ForvaltningUttrekkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt.FpoversiktMigreringRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fpoversikt.FpOversiktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.infotrygd.InfotrygdOppslagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.los.LosRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.register.RedirectToRegisterRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.InitielleLinksRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.VedtakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.VedtakJsonFeedRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.PipRestTjeneste;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

public class RestImplementationClasses {

    private RestImplementationClasses() {
    }

    public static Collection<Class<?>> getImplementationClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(InitielleLinksRestTjeneste.class);
        classes.add(FagsakRestTjeneste.class);
        classes.add(BehandlingRestTjeneste.class);
        classes.add(BehandlingRestTjenestePathHack1.class);
        classes.add(BehandlingBackendRestTjeneste.class);
        classes.add(BeregningsgrunnlagRestTjeneste.class);
        classes.add(AksjonspunktRestTjeneste.class);
        classes.add(DokumentRestTjeneste.class);
        classes.add(KodeverkRestTjeneste.class);
        classes.add(HistorikkRestTjeneste.class);
        classes.add(BatchRestTjeneste.class);
        classes.add(VedtakJsonFeedRestTjeneste.class);
        classes.add(FordelRestTjeneste.class);
        classes.add(HendelserRestTjeneste.class);
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
        classes.add(PipRestTjeneste.class);
        classes.add(TilbakekrevingRestTjeneste.class);
        classes.add(AktoerRestTjeneste.class);
        classes.add(OppdragRestTjeneste.class);
        classes.add(SvangerskapspengerRestTjeneste.class);
        classes.add(IAYRegisterdataCallbackRestTjeneste.class);
        classes.add(VergeRestTjeneste.class);
        classes.add(BrevRestTjeneste.class);
        classes.add(LosRestTjeneste.class);
        classes.add(RedirectToRegisterRestTjeneste.class);
        classes.add(OppgaverRestTjeneste.class);
        classes.add(FødselRestTjeneste.class);

        // Søk infotrygd
        classes.add(InfotrygdOppslagRestTjeneste.class);

        // Formidlingstjenester
        classes.add(FormidlingRestTjeneste.class);
        classes.add(BeregningsgrunnlagFormidlingRestTjeneste.class);
        classes.add(TilkjentYtelseFormidlingRestTjeneste.class);
        classes.add(ArbeidsforholdInntektsmeldingFormidlingRestTjeneste.class);

        classes.add(FpOversiktRestTjeneste.class);
        return Set.copyOf(classes);
    }

    public static Set<Class<?>> getForvaltningClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // UtilTjenester for uttrekk fra registre
        classes.add(ProsessTaskRestTjeneste.class);
        classes.add(DatavarehusAdminRestTjeneste.class);
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
}
