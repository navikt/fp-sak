package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/**
 * Grunnlag som inneholder tre versjoner av FamilieHendelse.
 * <p>
 * De forskjellige versjonene har kilde som følge:
 * 1: SøknadVersjon -> Søkers oppgitte data.
 * 2: BekreftetVersjon -> Bekreftede data fra registrene
 * 3: OverstyrtVersjon -> Saksbehandler overstyrer ved å behandle et aksjonspunkt.
 *
 * @see no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelse
 */
@Entity(name = FamilieHendelseGrunnlagEntitet.ENTITY_NAME)
@Table(name = "GR_FAMILIE_HENDELSE")
public class FamilieHendelseGrunnlagEntitet extends BaseEntitet {

    public static final String ENTITY_NAME = "FamilieHendelseGrunnlag";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_FAMILIE_HENDELSE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @OneToOne(optional = false)
    @JoinColumn(name = "soeknad_familie_hendelse_id", nullable = false, updatable = false, unique = true)
    @ChangeTracked
    private FamilieHendelseEntitet søknadHendelse;

    @OneToOne
    @JoinColumn(name = "bekreftet_familie_hendelse_id", updatable = false, unique = true)
    @ChangeTracked
    private FamilieHendelseEntitet bekreftetHendelse;

    @OneToOne
    @JoinColumn(name = "overstyrt_familie_hendelse_id", updatable = false, unique = true)
    @ChangeTracked
    private FamilieHendelseEntitet overstyrtHendelse;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    FamilieHendelseGrunnlagEntitet() {
    }

    FamilieHendelseGrunnlagEntitet(FamilieHendelseGrunnlagEntitet grunnlag) {
        this.søknadHendelse = grunnlag.getSøknadVersjon();
        grunnlag.getBekreftetVersjon().ifPresent(this::setBekreftetHendelse);
        grunnlag.getOverstyrtVersjon().ifPresent(this::setOverstyrtHendelse);
    }


    public Long getId() {
        return id;
    }

    /**
     * Søkers oppgitte data.
     *
     * @return FamilieHendelse
     */
    public FamilieHendelseEntitet getSøknadVersjon() {
        return søknadHendelse;
    }

    /**
     * Bekreftede data fra registrene
     *
     * @return Optional FamilieHendelse
     */
    public Optional<FamilieHendelseEntitet> getBekreftetVersjon() {
        return Optional.ofNullable(bekreftetHendelse);
    }


    public Long getBehandlingId() {
        return behandlingId;
    }

    void setBehandling(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        this.behandlingId = behandlingId;
    }

    /**
     * Saksbehandler overstyrer ved å behandle et aksjonspunkt.
     *
     * @return Optional FamilieHendelse
     */
    public Optional<FamilieHendelseEntitet> getOverstyrtVersjon() {
        return Optional.ofNullable(overstyrtHendelse);
    }


    public boolean getHarBekreftedeData() {
        return getGjeldendeBekreftetVersjon().isPresent();
    }

    public boolean getHarRegisterData() {
        return getBekreftetVersjon().map(FamilieHendelseEntitet::getType).filter(FamilieHendelseType.FØDSEL::equals).isPresent();
    }

    public boolean getHarOverstyrteData() {
        return getOverstyrtVersjon().isPresent();
    }

    /**
     * Gir den mest relevante versjonen avhengig av hva som er tilstede.
     * <ol>
     * <li>Overstyrt versjon</li>
     * <li>Register versjon</li>
     * <li>Søknad versjon</li>
     * </ol>
     *
     * @return FamilieHendelse
     */
    public FamilieHendelseEntitet getGjeldendeVersjon() {
        if (getOverstyrtVersjon().isPresent()) {
            return overstyrtHendelse;
        }
        if (getBekreftetVersjon().isPresent()) {
            return bekreftetHendelse;
        }
        return søknadHendelse;
    }

    /**
     * Gir den mest relevante versjonen av Adopsjon avhengig av hva som er tilstede.
     * <ol>
     * <li>Overstyrt versjon</li>
     * <li>Register versjon</li>
     * <li>Søknad versjon</li>
     * </ol>
     *
     * @return adopsjon
     */
    public Optional<AdopsjonEntitet> getGjeldendeAdopsjon() {
        var overstyrt = getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getAdopsjon);
        if (overstyrt.isPresent()) {
            return overstyrt;
        }
        var bekreftet = getBekreftetVersjon().flatMap(FamilieHendelseEntitet::getAdopsjon);
        if (bekreftet.isPresent()) {
            return bekreftet;
        }
        return getSøknadVersjon().getAdopsjon();
    }

    /**
     * Gir den mest relevante versjonen av Terminbekreftelse avhengig av hva som er tilstede.
     * <ol>
     * <li>Overstyrt versjon</li>
     * <li>Register versjon</li>
     * <li>Søknad versjon</li>
     * </ol>
     *
     * @return Terminbekreftelse
     */
    public Optional<TerminbekreftelseEntitet> getGjeldendeTerminbekreftelse() {
        var overstyrt = getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTerminbekreftelse);
        if (overstyrt.isPresent()) {
            return overstyrt;
        }
        var bekreftet = getBekreftetVersjon().flatMap(FamilieHendelseEntitet::getTerminbekreftelse);
        if (bekreftet.isPresent()) {
            return bekreftet;
        }
        return getSøknadVersjon().getTerminbekreftelse();
    }

    /**
     * Gir den mest relevante versjonen av Barn avhengig av hva som er tilstede.
     * 1: Overstyrt versjon hvis tilstede og har innhold
     * 2: Register versjon hvis tilstede og har innhold
     * 3: Søknad versjon
     *
     * @return Liste av UidentifisertBarn
     */
    public List<UidentifisertBarn> getGjeldendeBarna() {
        var overstyrt = getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        if (!overstyrt.isEmpty()) {
            return overstyrt;
        }
        var bekreftet = getBekreftetVersjon().map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList());
        if (!bekreftet.isEmpty()) {
            return bekreftet;
        }
        return søknadHendelse.getBarna();
    }

    /**
     * Gir den mest relevante versjonen av antall barn avhengig av hva som er tilstede.
     * 1: Overstyrt versjon
     * 2: Register versjon
     * 3: Søknad versjon
     *
     * @return antall barn
     */
    public Integer getGjeldendeAntallBarn() {
        var overstyrt = getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn);
        if (overstyrt.isPresent()) {
            return overstyrt.get();
        }
        var bekreftet = getBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn);
        return bekreftet.orElseGet(() -> getSøknadVersjon().getAntallBarn());
    }

    /**
     * Gir den mest relevante versjonen avhengig av hva som er tilstede.
     * 1: Overstyrt versjon
     * 2: Register versjon
     *
     * @return FamilieHendelse
     */
    public Optional<FamilieHendelseEntitet> getGjeldendeBekreftetVersjon() {
        if (getOverstyrtVersjon().isPresent()) {
            return Optional.of(overstyrtHendelse);
        }
        return Optional.ofNullable(bekreftetHendelse);
    }

    /**
     * Gir den mest relevante versjonen avhengig av hva som er tilstede.
     * <ol>
     * <li>Overstyrt versjon</li>
     * <li>Register versjon</li>
     * <li>Søknad versjon</li>
     * </ol>
     *
     * @return foreslått Fødselsdatoen
     */
    public LocalDate finnGjeldendeFødselsdato() {
        var bekreftetVersjon = getGjeldendeBekreftetVersjon();

        var bekreftetBarna = bekreftetVersjon.map(FamilieHendelseEntitet::getBarna).orElse(List.of());
        if (!bekreftetBarna.isEmpty()) {
            return bekreftetBarna.stream().map(UidentifisertBarn::getFødselsdato).min(Comparator.naturalOrder()).orElseThrow();
        }

        var søknadBarna = søknadHendelse.getBarna();
        if (!søknadBarna.isEmpty()) {
            return søknadBarna.stream().map(UidentifisertBarn::getFødselsdato).min(Comparator.naturalOrder()).orElseThrow();
        }
        return getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato)
            .orElse(søknadHendelse.getTerminbekreftelse().orElseThrow().getTermindato());
    }

    public boolean getErAktivt() {
        return aktiv;
    }

    void setSøknadHendelse(FamilieHendelseEntitet soeknadHendelse) {
        this.søknadHendelse = soeknadHendelse;
    }

    void setBekreftetHendelse(FamilieHendelseEntitet registerHendelse) {
        this.bekreftetHendelse = registerHendelse;
    }

    void setOverstyrtHendelse(FamilieHendelseEntitet overstyrtHendelse) {
        this.overstyrtHendelse = overstyrtHendelse;
    }

    void setAktiv(boolean aktivt) {
        this.aktiv = aktivt;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (FamilieHendelseGrunnlagEntitet) o;
        return aktiv == that.aktiv &&
                Objects.equals(søknadHendelse, that.søknadHendelse) &&
                Objects.equals(bekreftetHendelse, that.bekreftetHendelse) &&
                Objects.equals(overstyrtHendelse, that.overstyrtHendelse);
    }


    @Override
    public int hashCode() {
        return Objects.hash(søknadHendelse, bekreftetHendelse, overstyrtHendelse, aktiv);
    }
}
