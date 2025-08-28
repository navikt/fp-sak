package no.nav.foreldrepenger.familiehendelse;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;
import static no.nav.foreldrepenger.familiehendelse.modell.FødselStatus.safeGet;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.modell.FødselStatus;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
public class FaktaFødselTjeneste {
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    FaktaFødselTjeneste() {
        // For CDI proxy
    }

    @Inject
    public FaktaFødselTjeneste(FamilieHendelseTjeneste familieHendelseTjeneste, OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
    }

    private static void validerGyldigTerminFødsel(Optional<LocalDate> termindato, Optional<List<DokumentertBarnDto>> barna) {
        var fødselsdato = barna.orElse(List.of())
            .stream()
            .map(DokumentertBarnDto::fødselsdato)
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder());
        if (termindato.isPresent() && fødselsdato.isPresent()) {
            var fødselsintervall = FamilieHendelseTjeneste.intervallForTermindato(termindato.get());
            if (!fødselsintervall.encloses(fødselsdato.get())) {
                throw new FunksjonellException("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
            }
        }
    }

    private static void validerDødsdatoerMotFødselsdatoer(Optional<List<DokumentertBarnDto>> barna) {
        barna.ifPresent(barn -> barn.forEach(b -> {
            if (b.dødsdato() != null && b.dødsdato().isBefore(b.fødselsdato())) {
                throw new FunksjonellException("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
            }
        }));
    }

    public Historikkinnslag.Builder lagHistorikkForBarn(BehandlingReferanse ref,
                                                        FamilieHendelseGrunnlagEntitet fh,
                                                        Optional<LocalDate> termindato,
                                                        Optional<List<DokumentertBarnDto>> barna,
                                                        String begrunnelse,
                                                        boolean erOverstyring) {
        var historikkinnslag = new Historikkinnslag.Builder().medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL);

        if (erOverstyring) {
            historikkinnslag.addLinje(new HistorikkinnslagLinjeBuilder().bold("Overstyrt fakta om fødsel"));

            var gjeldendeTermindato = fh.getGjeldendeVersjon().getTermindato().orElse(null);
            termindato.filter(t -> !t.equals(gjeldendeTermindato))
                .ifPresent(t -> historikkinnslag.addLinje(
                    fraTilEquals("Termindato", fh.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null), t)));
        }

        var finnesFødteBarn = barna.isPresent();
        historikkinnslag.addLinje(new HistorikkinnslagLinjeBuilder().bold("Er barnet født?").tekst(format(finnesFødteBarn)));

        if (barna.isPresent()) {
            var oppdatertFødselStatus = barna.get().stream().map(FødselStatus::new).toList();
            var gjeldendeFødselStatus = fh.getGjeldendeBarna().stream().map(FødselStatus::new).toList();

            if (!Objects.equals(oppdatertFødselStatus.size(), fh.getGjeldendeAntallBarn())) {
                historikkinnslag.addLinje(
                    new HistorikkinnslagLinjeBuilder().fraTil("Antall barn", fh.getGjeldendeAntallBarn(), oppdatertFødselStatus.size()));
            } else {
                historikkinnslag.addLinje(new HistorikkinnslagLinjeBuilder().bold("Antall barn:").tekst(format(oppdatertFødselStatus.size())));
            }

            if (!oppdatertFødselStatus.equals(gjeldendeFødselStatus)) {
                var lengsteListeStørrelse = Math.max(oppdatertFødselStatus.size(), gjeldendeFødselStatus.size());
                for (int i = 0; i < lengsteListeStørrelse; i++) {
                    var til = safeGet(oppdatertFødselStatus, i).map(FødselStatus::formaterLevetid).orElse(null);
                    var fra = safeGet(gjeldendeFødselStatus, i).map(FødselStatus::formaterLevetid).orElse(null);
                    var barn = lengsteListeStørrelse > 1 ? "Barn " + (i + 1) : "Barn";
                    historikkinnslag.addLinje(fraTilEquals(barn, fra, til));
                }
            }
        }
        historikkinnslag.addLinje(begrunnelse);
        return historikkinnslag;
    }


    public OppdateringResultat overstyrFaktaOmFødsel(BehandlingReferanse ref,
                                                     FamilieHendelseGrunnlagEntitet familieHendelse,
                                                     Optional<LocalDate> termindato,
                                                     Optional<List<DokumentertBarnDto>> barna) {
        validerDødsdatoerMotFødselsdatoer(barna);
        validerGyldigTerminFødsel(termindato, barna);

        var behandlingId = ref.behandlingId();
        var oppdatere = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId);

        if (barna.isPresent()) {
            oppdatere.medFødselType().tilbakestillBarn().medAntallBarn(barna.get().size());
            barna.get().forEach(b -> oppdatere.leggTilBarn(b.fødselsdato(), b.dødsdato()));
        } else {

            familieHendelse.getBekreftetVersjon().ifPresentOrElse(bv -> {

                var bekreftetBarn = bv.getBarna();
                var bekreftetAntallBarn = bv.getAntallBarn();

                oppdatere.medTerminType().tilbakestillBarn().medAntallBarn(bekreftetAntallBarn);
                bekreftetBarn.forEach(b -> oppdatere.leggTilBarn(b.getFødselsdato(), b.getDødsdato().orElse(null)));
            }, () -> {
                var søknadBarn = familieHendelse.getSøknadVersjon().getBarna();
                var søknadAntallBarn = familieHendelse.getSøknadVersjon().getAntallBarn();

                oppdatere.medTerminType().tilbakestillBarn().medAntallBarn(søknadAntallBarn);

                søknadBarn.forEach(b -> oppdatere.leggTilBarn(b.getFødselsdato(), b.getDødsdato().orElse(null)));
            });
        }

        termindato.ifPresent(date -> {
            var tbb = oppdatere.getTerminbekreftelseBuilder().medTermindato(date);
            oppdatere.medTerminbekreftelse(tbb);
        });

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatere);

        return getOppdateringResultat(ref, behandlingId);
    }

    private OppdateringResultat getOppdateringResultat(BehandlingReferanse ref, Long behandlingId) {
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, ref.fagsakYtelseType());
        var sisteFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, ref.fagsakYtelseType());


        if (Objects.equals(forrigeFikspunkt, sisteFikspunkt)) {
            return OppdateringResultat.utenTransisjon().medTotrinn().build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinn().medOppdaterGrunnlag().build();
        }
    }
}
