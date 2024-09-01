package no.nav.foreldrepenger.inngangsvilkaar.opptjening.fp;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.Inngangsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultatOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.InngangsvilkårRegler;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelYtelse;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.LovVersjoner;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

@ApplicationScoped
@VilkårTypeRef(VilkårType.OPPTJENINGSPERIODEVILKÅR)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class InngangsvilkårOpptjeningsperiode implements Inngangsvilkår {

    private FamilieHendelseRepository familieHendelseRepository;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    InngangsvilkårOpptjeningsperiode() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårOpptjeningsperiode(FamilieHendelseRepository familieHendelseRepository, YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.ytelseMaksdatoTjeneste = ytelseMaksdatoTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public VilkårData vurderVilkår(BehandlingReferanse ref) {
        var grunnlag = opprettGrunnlag(ref);

        var resultat = InngangsvilkårRegler.opptjeningsperiode(RegelYtelse.FORELDREPENGER, grunnlag);

        return RegelResultatOversetter.oversett(VilkårType.OPPTJENINGSPERIODEVILKÅR, resultat);
    }

    private OpptjeningsperiodeGrunnlag opprettGrunnlag(BehandlingReferanse ref) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        var behandlingId = ref.behandlingId();
        var hendelseAggregat = familieHendelseRepository.hentAggregat(behandlingId);
        var hendelse = hendelseAggregat.getGjeldendeVersjon();
        var førsteUttaksdato = stp.getFørsteUttaksdato();
        var lovversjon = stp.utenMinsterett() ? LovVersjoner.KLASSISK : LovVersjoner.PROP15L2122;

        var fagsakÅrsak = finnFagsakÅrsak(hendelse);
        var søkerRolle = finnFagsakSøkerRolle(ref);
        Optional<LocalDate> morsMaksdato = UtsettelseCore2021.kreverSammenhengendeUttak(hendelseAggregat) ?
            ytelseMaksdatoTjeneste.beregnMorsMaksdato(ref.saksnummer(), ref.relasjonRolle())
                .filter(UtsettelseCore2021::kreverSammenhengendeUttakMorsMaxdato) : Optional.empty();

        Optional<LocalDate> termindato;
        LocalDate hendelsedato;
        if (FagsakÅrsak.FØDSEL.equals(fagsakÅrsak)) {
            termindato = hendelseAggregat.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
            hendelsedato = hendelseAggregat.finnGjeldendeFødselsdato();
        } else {
            termindato = Optional.empty();
            hendelsedato = hendelse.getSkjæringstidspunkt();
        }

        if (fagsakÅrsak == null || søkerRolle == null) {
            throw new IllegalArgumentException("Utvikler-feil: Finner ikke årsak/rolle for behandling:" + behandlingId);
        }
        if (hendelsedato == null) {
            throw new IllegalArgumentException("Utvikler-feil: Finner ikke hendelsesdato for behandling:" + behandlingId);
        }

        return OpptjeningsperiodeGrunnlag.grunnlag(fagsakÅrsak, søkerRolle, lovversjon)
            .medFørsteUttaksDato(førsteUttaksdato)
            .medHendelsesDato(hendelsedato)
            .medTerminDato(termindato.orElse(null))
            .medMorsMaksdato(morsMaksdato.orElse(null));
    }

    private RegelSøkerRolle finnFagsakSøkerRolle(BehandlingReferanse ref) {
        var relasjonsRolleType = ref.relasjonRolle();
        if (RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            return RegelSøkerRolle.MORA;
        }
        if (RelasjonsRolleType.UDEFINERT.equals(relasjonsRolleType) || RelasjonsRolleType.BARN.equals(relasjonsRolleType)) {
            return null;
        }
        return RegelSøkerRolle.FARA;
    }

    private FagsakÅrsak finnFagsakÅrsak(FamilieHendelseEntitet familieHendelse) {
        var type = familieHendelse.getType();
        if (familieHendelse.getGjelderFødsel()) {
            return FagsakÅrsak.FØDSEL;
        }
        if (FamilieHendelseType.ADOPSJON.equals(type)) {
            return FagsakÅrsak.ADOPSJON;
        }
        if (FamilieHendelseType.OMSORG.equals(type)) {
            return FagsakÅrsak.OMSORG;
        }
        return null;
    }
}
